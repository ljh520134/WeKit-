package moe.ouom.wekit.hooks.items.payment

import android.content.Context
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import moe.ouom.wekit.config.WeConfig
import moe.ouom.wekit.core.dsl.dexMethod
import moe.ouom.wekit.core.model.BaseClickableFunctionHookItem
import moe.ouom.wekit.dexkit.intf.IDexFind
import moe.ouom.wekit.hooks.core.annotation.HookItem
import moe.ouom.wekit.ui.utils.showComposeDialog
import org.luckypray.dexkit.DexKitBridge

@HookItem(path = "红包与支付/修改显示余额", desc = "伪装钱包余额文字")
object ModifyWalletBalanceDisplay : BaseClickableFunctionHookItem(), IDexFind {

    private val config = WeConfig.getDefaultConfig()
    private const val KEY_BALANCE = "fake_wallet_balance"
    private const val DEFAULT_BALANCE = "¥8,888,888.88"

    private val methodUpdateBalanceDisplay by dexMethod()

    override fun entry(classLoader: ClassLoader) {
        methodUpdateBalanceDisplay.toDexMethod {
            hook {
                afterIfEnabled { param ->
                    val balanceView = param.thisObject.asResolver()
                        .firstField { type = TextView::class }
                        .get()!! as TextView
                    balanceView.text = config.getStringPrek(KEY_BALANCE, DEFAULT_BALANCE) ?: DEFAULT_BALANCE
                }
            }
        }
    }

    override fun dexFind(dexKit: DexKitBridge): Map<String, String> {
        val descriptors = mutableMapOf<String, String>()

        methodUpdateBalanceDisplay.find(dexKit, descriptors) {
            matcher {
                declaredClass = "com.tencent.mm.plugin.mall.ui.MallIndexUIv2"
                usingEqStrings("MicorMsg.MallIndexUIv2", "updateBalanceNum")
            }
        }

        return descriptors
    }

    override fun onClick(context: Context) {
        showComposeDialog(context) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {

            }
        }
    }
}