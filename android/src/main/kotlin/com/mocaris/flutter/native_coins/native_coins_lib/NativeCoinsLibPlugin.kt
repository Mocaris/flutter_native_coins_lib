package com.mocaris.flutter.native_coins.native_coins_lib

import android.content.Context
import androidx.annotation.NonNull
import coins.wallet.CoinWallet
import coins.wallet.WalletFactory
import coins.wallet.coin.CoinType
import coins.wallet.mnemonic.MnemonicUtil

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import kotlinx.coroutines.*
import java.lang.Exception
import kotlin.coroutines.coroutineContext

/** NativeCoinsLibPlugin */
class NativeCoinsLibPlugin : FlutterPlugin, MethodCallHandler {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private lateinit var channel: MethodChannel
    private lateinit var applicationContext: Context

    private val walletFactory: WalletFactory = WalletFactory()

    private val pluginScope: CoroutineScope = CoroutineScope(Dispatchers.Default)

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "native_coins_lib")

        applicationContext = flutterPluginBinding.applicationContext
        channel.setMethodCallHandler(this)
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        when (call.method) {
            "initWalletConfig" -> {
                val testNet = call.argument<Boolean>("testNet") ?: false
                CoinWallet.config.testNet = testNet
                result.success(true)
            }
            "generateMnemonic" -> {
                result.success(MnemonicUtil.createMnemonicWord())
            }
            "getAllMnemonicWords" -> {
                result.success(MnemonicUtil.getAllMnemonicWord())
            }
            "checkMnemonicWords" -> {
                checkMnemonicWords(call, result)
            }
            "getAddress" -> {
                getAddress(call, result)
            }
            "checkAddress" -> {
                checkAddress(call, result)
            }
            "signTransaction" -> {
                signTransaction(call, result)
            }
            else -> result.notImplemented()
        }
    }

    private fun checkMnemonicWords(call: MethodCall, result: Result) {
        val words = call.argument<List<String>>("words")
        if (null != words) {
            result.success(MnemonicUtil.checkMnemonic(words))
        } else {
            result.error("0", "words can not be null", "checkMnemonicWords")
        }
    }

    private fun signTransaction(call: MethodCall, result: Result) {
        val words = call.argument<List<String>>("words")
        val coinName = call.argument<String>("coinName")
        val passPhrase = call.argument<String>("passPhrase")
        val inputTransaction = call.argument<String>("inputTransaction")
        val addressTo = call.argument<String>("addressTo")
        if (null == words || null == inputTransaction || null == addressTo || null == coinName) {
            result.error("0", "words and coinName and addressTo and inputTransaction can not be null", "signTransaction")
        } else {
            val coinWallet = walletFactory.getCoinWallet(CoinType.valueOf(coinName))
            pluginScope.launch(Dispatchers.IO) {
                try {
                    val signHash = coinWallet.signTransaction(inputTransaction, addressTo, words, passPhrase ?: "")
                    withContext(Dispatchers.Main) {
                        result.success(signHash)
                    }
                } catch (e: Exception) {
                    result.error("0", e.message, "checkAddress")
                }
            }
        }
    }

    private fun checkAddress(call: MethodCall, result: Result) {
        val address = call.argument<String>("address")
        val coinName = call.argument<String>("coinName")
        if (null != address && null != coinName) {
            try {
                val coinWallet = walletFactory.getCoinWallet(CoinType.valueOf(coinName))
                coinWallet.checkAddress(address)
            } catch (e: Exception) {
                result.error("0", e.message, "checkAddress")
            }
        } else {
            result.error("0", "words and coinName can not be null", "getAddress")
        }
    }

    private fun getAddress(call: MethodCall, result: Result) {
        val words = call.argument<List<String>>("words")
        val coinName = call.argument<String>("coinName")
        val passPhrase = call.argument<String>("passPhrase")
        if (null != words && null != coinName) {
            val coinWallet = walletFactory.getCoinWallet(CoinType.valueOf(coinName))
            pluginScope.launch(Dispatchers.IO) {
                try {
                    val address = coinWallet.generateAddress(words, passPhrase ?: "")
                    withContext(Dispatchers.Main) {
                        result.success(address)
                    }
                } catch (e: Exception) {
                    result.error("0", e.message, "getAddress")
                }
            }
        } else {
            result.error("0", "words and coinName can not be null", "getAddress")
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        pluginScope.cancel()
        channel.setMethodCallHandler(null)
    }
}
