package com.kurobello.tallybook.core.data

import com.kurobello.tallybook.core.model.DataOutcome

/**
 * Device-scoped identity used as the node of every
 * [com.kurobello.tallybook.core.model.ChangeStamp].
 */
interface InstallationIdRepository {
  /** 16 lowercase hex chars, created on first read and never restored onto another device. */
  suspend fun nodeId(): DataOutcome<String>
}
