package com.kurobello.tallybook.core.data.entry

import com.kurobello.tallybook.core.data.CategorySeedCatalog
import com.kurobello.tallybook.core.model.CATEGORIA_OTROS_ID
import com.kurobello.tallybook.core.model.TipoMovimiento
import kotlin.math.pow
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/** One saved movimiento: its parsed subject and every categoria it carries, in display order. */
data class CategoryHistoryEntry(
    val subject: String,
    val categoriaIds: List<String>,
    val createdAt: Instant,
)

enum class SuggestionConfidence {
  HIGH,
  MEDIUM,
}

data class CategorySuggestion(val categoriaId: String, val confidence: SuggestionConfidence)

class CategorySuggester(private val dictionary: CategoryKeywordDictionary) {

  fun index(history: List<CategoryHistoryEntry>, now: Instant): CategorySuggestionIndex {
    val exact = mutableMapOf<String, CategoryMass>()
    val exactSets = mutableMapOf<String, CategorySetMass>()
    val byToken = mutableMapOf<String, CategoryMass>()
    history.forEach { entry ->
      val tokens = suggestionTokens(entry.subject)
      // Otros means "not categorized yet", so it carries no signal about what the words mean.
      val categoriaIds = entry.categoriaIds.filter { it != CATEGORIA_OTROS_ID }.distinct()
      if (tokens.isNotEmpty() && categoriaIds.isNotEmpty()) {
        // A movimiento is one vote, however many categorias it carries.
        val weight = recencyWeight(entry.createdAt, now)
        val share = weight / categoriaIds.size
        val key = subjectKey(tokens)
        exact.getOrPut(key) { CategoryMass() }.addAll(categoriaIds, share)
        exactSets.getOrPut(key) { CategorySetMass() }.add(categoriaIds, weight)
        tokens.toSet().forEach { token ->
          byToken.getOrPut(token) { CategoryMass() }.addAll(categoriaIds, share)
        }
      }
    }
    return CategorySuggestionIndex(dictionary, exact, exactSets, byToken)
  }
}

class CategorySuggestionIndex
internal constructor(
    private val dictionary: CategoryKeywordDictionary,
    private val exact: Map<String, CategoryMass>,
    private val exactSets: Map<String, CategorySetMass>,
    private val byToken: Map<String, CategoryMass>,
) {

  /** Ranked, at most [limit]; only the first element may be HIGH. Never returns excluded IDs. */
  fun suggest(
      subject: String,
      tipo: TipoMovimiento?,
      availableIds: Set<String>,
      excludedIds: Set<String>,
      limit: Int = DEFAULT_LIMIT,
  ): List<CategorySuggestion> {
    val tokens = suggestionTokens(subject)
    if (limit <= 0 || tokens.isEmpty()) return emptyList()
    val ranked =
        evidence(tokens)
            .filter { (id, _) ->
              id in availableIds && id !in excludedIds && id != CATEGORIA_OTROS_ID
            }
            .map { (id, candidate) -> candidate.toRanked(id, tipoFactor(id, tipo)) }
            .filter { it.score >= MIN_SCORE }
            .sortedWith(compareByDescending<Ranked> { it.score }.thenBy { it.categoriaId })
    return ranked.take(limit).mapIndexed { position, candidate ->
      val confidence =
          if (position == 0 && isHighConfidence(ranked)) SuggestionConfidence.HIGH
          else SuggestionConfidence.MEDIUM
      CategorySuggestion(candidate.categoriaId, confidence)
    }
  }

  /**
   * The categoria set that a clear majority of the past movimientos with exactly this subject were
   * saved with, in its saved order, or empty when no set has that majority. Unavailable and
   * excluded ids are left out of the result.
   */
  fun learnedSet(
      subject: String,
      availableIds: Set<String>,
      excludedIds: Set<String>,
  ): List<String> {
    val mass = exactSets[subjectKey(suggestionTokens(subject))] ?: return emptyList()
    return mass.majority(CLEAR_MAJORITY).filter { it in availableIds && it !in excludedIds }
  }

  private fun evidence(tokens: List<String>): Map<String, Evidence> {
    val evidence = mutableMapOf<String, Evidence>()
    exact[subjectKey(tokens)]?.let { mass ->
      mass.byCategory.forEach { (id, weight) ->
        evidence
            .getOrPut(id) { Evidence() }
            .apply {
              score += EXACT_SCALE * weight / (mass.total + EXACT_SMOOTHING)
              exactShare = weight / mass.total
            }
      }
    }
    tokens.toSet().forEach { token ->
      byToken[token]?.let { mass ->
        mass.byCategory.forEach { (id, weight) ->
          evidence.getOrPut(id) { Evidence() }.score +=
              LEARNED_SCALE * weight / (mass.total + LEARNED_SMOOTHING)
        }
      }
    }
    dictionary.evidence(tokens).forEach { (id, hit) ->
      evidence
          .getOrPut(id) { Evidence() }
          .apply {
            score += hit.score
            strongDictionary = hit.strong
          }
    }
    return evidence
  }

  private fun isHighConfidence(ranked: List<Ranked>): Boolean {
    val top = ranked.first()
    val runnerUp = ranked.getOrNull(1)
    val leadsClearly = runnerUp == null || runnerUp.score < top.score * COMPETITION_RATIO
    return top.exactShare >= CLEAR_MAJORITY || (top.strongDictionary && leadsClearly)
  }

  private fun tipoFactor(categoriaId: String, tipo: TipoMovimiento?): Double =
      if (categoriaId in incomeCategoryIds) tipo?.let(incomeCategoryFactor::get) ?: 1.0 else 1.0

  private fun Evidence.toRanked(categoriaId: String, factor: Double) =
      Ranked(categoriaId, score * factor, exactShare, strongDictionary)

  private class Evidence {
    var score = 0.0
    var exactShare = 0.0
    var strongDictionary = false
  }

  private class Ranked(
      val categoriaId: String,
      val score: Double,
      val exactShare: Double,
      val strongDictionary: Boolean,
  )
}

internal class CategoryMass {
  var total = 0.0
    private set

  val byCategory = mutableMapOf<String, Double>()

  fun addAll(categoriaIds: List<String>, weight: Double) {
    categoriaIds.forEach { categoriaId ->
      total += weight
      byCategory[categoriaId] = (byCategory[categoriaId] ?: 0.0) + weight
    }
  }
}

/** Votes per whole categoria set; the first-seen order of a set is the one it is returned in. */
internal class CategorySetMass {
  private var total = 0.0
  private val votes = mutableMapOf<Set<String>, Vote>()

  fun add(categoriaIds: List<String>, weight: Double) {
    total += weight
    votes.getOrPut(categoriaIds.toSet()) { Vote(categoriaIds) }.weight += weight
  }

  fun majority(minimumShare: Double): List<String> =
      votes.values.firstOrNull { it.weight / total >= minimumShare }?.categoriaIds.orEmpty()

  private class Vote(val categoriaIds: List<String>) {
    var weight = 0.0
  }
}

private fun recencyWeight(createdAt: Instant, now: Instant): Double {
  val ageDays = ((now - createdAt) / 1.days).coerceAtLeast(0.0)
  return 0.5.pow(ageDays / HALF_LIFE_DAYS)
}

private const val INCOME_PARENT_ID = "cat_ingresos"

internal val incomeCategoryIds: Set<String> =
    CategorySeedCatalog.categories
        .filter { it.id == INCOME_PARENT_ID || it.padreId == INCOME_PARENT_ID }
        .map { it.id }
        .toSet()

private val incomeCategoryFactor: Map<TipoMovimiento, Double> =
    mapOf(TipoMovimiento.INGRESO to INCOME_BOOST, TipoMovimiento.GASTO to EXPENSE_PENALTY)

private const val DEFAULT_LIMIT = 3
private const val HALF_LIFE_DAYS = 90.0
private const val EXACT_SCALE = 12.0
private const val EXACT_SMOOTHING = 0.5
private const val LEARNED_SCALE = 10.0
private const val LEARNED_SMOOTHING = 1.0
private const val MIN_SCORE = 1.0
private const val CLEAR_MAJORITY = 0.65
private const val COMPETITION_RATIO = 0.5
private const val INCOME_BOOST = 1.5
private const val EXPENSE_PENALTY = 0.3
