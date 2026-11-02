package com.alosir.card.ui.screen.pin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alosir.card.data.security.SecurityManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** PIN 页模式 */
enum class PinMode {
    /** 设置新密码（输入两次，PRD F8.2） */
    SETUP,
    /** 修改密码：先验旧、再设新 */
    CHANGE,
    /** 验证解锁（应用锁/查看卡号闸门） */
    VERIFY,
}

data class PinUiState(
    val mode: PinMode = PinMode.SETUP,
    val input: String = "",
    /** SETUP/CHANGE 的第二阶段：确认新密码 */
    val confirmInput: String = "",
    val stage: Int = 1,
    val error: String? = null,
    val lockedSeconds: Int = 0,
    val hint: String = "",
    /** PBKDF2 校验在后台线程进行，界面显示「验证中…」而非卡死 */
    val verifying: Boolean = false,
    val done: Boolean = false,
) {
    val display: String get() = if (stage == 2) confirmInput else input
}

@HiltViewModel
class PinViewModel @Inject constructor(
    private val security: SecurityManager,
) : ViewModel() {

    private val pinLength = SecurityManager.PIN_LENGTH

    private val _ui = MutableStateFlow(PinUiState())
    val ui: StateFlow<PinUiState> = _ui.asStateFlow()

    fun configure(mode: PinMode) {
        _ui.update {
            it.copy(
                mode = mode,
                hint = when (mode) {
                    PinMode.SETUP -> "设置 $pinLength 位数字安全密码"
                    PinMode.CHANGE -> "先输入当前密码"
                    PinMode.VERIFY -> "输入 $pinLength 位安全密码"
                },
            )
        }
    }

    fun onKey(key: String) {
        val state = _ui.value
        if (state.lockedSeconds > 0 || state.verifying) return
        if (key == "del") {
            backspace()
            return
        }
        if (!key.all { it.isDigit() }) return
        val current = state.display
        if (current.length >= pinLength) return
        commit(current + key)
    }

    fun onBackspace() = backspace()

    private fun backspace() {
        val state = _ui.value
        if (state.stage == 2) {
            _ui.update { it.copy(confirmInput = it.confirmInput.dropLast(1), error = null) }
        } else {
            _ui.update { it.copy(input = it.input.dropLast(1), error = null) }
        }
    }

    private fun commit(value: String) {
        val state = _ui.value
        // 先更新输入态（第 4 位立即点亮），再进入校验——绝不把校验耗时暴露在输入反馈之前
        _ui.update {
            if (state.stage == 2) it.copy(confirmInput = value) else it.copy(input = value)
        }
        if (value.length < pinLength) return

        when (state.mode) {
            PinMode.VERIFY -> verifyAsync(value)
            PinMode.SETUP -> if (state.stage == 1) {
                _ui.update { it.copy(input = value, stage = 2, confirmInput = "", error = null, hint = "再输一次确认") }
            } else {
                if (value == state.input) {
                    setPinAsync(value)
                } else {
                    _ui.update {
                        it.copy(stage = 1, input = "", confirmInput = "", error = "两次输入不一致，请重设")
                    }
                }
            }
            PinMode.CHANGE -> when (state.stage) {
                1 -> verifyForChange(value)
                2 -> _ui.update {
                    it.copy(input = value, stage = 3, confirmInput = "", hint = "再输一次确认", error = null)
                }
                else -> {
                    if (value == state.input) {
                        setPinAsync(value)
                    } else {
                        _ui.update {
                            it.copy(stage = 2, input = "", confirmInput = "", error = "两次输入不一致，请重设")
                        }
                    }
                }
            }
        }
    }

    /** PBKDF2 约 200–400ms，必须离开主线程 */
    private fun verifyAsync(pin: String) {
        _ui.update { it.copy(verifying = true, error = null) }
        viewModelScope.launch(Dispatchers.Default) {
            when (val result = security.verifyPin(pin)) {
                is SecurityManager.VerifyResult.Success ->
                    _ui.update { it.copy(done = true, verifying = false, error = null) }
                is SecurityManager.VerifyResult.Wrong ->
                    _ui.update {
                        it.copy(input = "", verifying = false, error = "安全密码错误，还可试 ${result.remainingAttempts} 次")
                    }
                is SecurityManager.VerifyResult.Locked ->
                    _ui.update { it.copy(input = "", verifying = false, lockedSeconds = result.remainingSeconds) }
            }
        }
    }

    private fun verifyForChange(pin: String) {
        _ui.update { it.copy(verifying = true, error = null) }
        viewModelScope.launch(Dispatchers.Default) {
            when (val result = security.verifyPin(pin)) {
                is SecurityManager.VerifyResult.Success ->
                    _ui.update {
                        it.copy(input = "", stage = 2, hint = "设置新的 $pinLength 位密码", verifying = false, error = null)
                    }
                is SecurityManager.VerifyResult.Wrong ->
                    _ui.update {
                        it.copy(input = "", verifying = false, error = "密码错误，还可试 ${result.remainingAttempts} 次")
                    }
                is SecurityManager.VerifyResult.Locked ->
                    _ui.update { it.copy(input = "", verifying = false, lockedSeconds = result.remainingSeconds) }
            }
        }
    }

    private fun setPinAsync(pin: String) {
        _ui.update { it.copy(verifying = true) }
        viewModelScope.launch(Dispatchers.Default) {
            security.setPin(pin)
            security.unlockSession()
            _ui.update { it.copy(done = true, verifying = false, error = null) }
        }
    }

    fun tickLock() {
        val remaining = security.lockRemainingSeconds()
        _ui.update { it.copy(lockedSeconds = remaining) }
    }
}
