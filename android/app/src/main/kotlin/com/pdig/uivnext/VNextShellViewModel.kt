package com.pdig.uivnext

import androidx.lifecycle.ViewModel

/**
 * vNext 演示壳状态宿主：Activity 重建（旋转 / 进程保留）后保持导航 / 选择 / 投影状态
 * （任务书 §32 状态恢复；演示壳内无真实数据，ViewModel 生命周期即恢复边界）。
 *
 * 恢复策略：
 *  - 恢复：selected primary nav、selected infra tab（regionFilter）、selected object、
 *    changeProjection、privacyMask、reduceMotion、railExpanded、globe camera。
 *  - 不恢复：Studio 未保存编辑（remember 局部状态，明确不持久化）、搜索词。
 *  - 需要明确确认：无（演示壳无破坏性写操作）。
 */
class VNextShellViewModel : ViewModel() {
    val app = createVNextAppState()
}
