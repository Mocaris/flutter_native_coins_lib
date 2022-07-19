package com.mocaris.flutter.native_coins.native_coins_lib

import androidx.annotation.NonNull

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import kotlinx.coroutines.*
import org.mocaris.coins.wallet.CoinWallet
import org.mocaris.coins.wallet.WalletFactory
import org.mocaris.coins.wallet.coin.CoinType
import org.mocaris.coins.wallet.imp.*
import org.mocaris.coins.wallet.mnemonic.MnemonicUtil
import java.lang.Exception
import kotlin.coroutines.coroutineContext

/** NativeCoinsLibPlugin */
class NativeCoinsLibPlugin : FlutterPlugin, MethodCallHandler {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private lateinit var channel: MethodChannel

    private val walletFactory: WalletFactory = WalletFactory()

    private val pluginScope: CoroutineScope = CoroutineScope(Dispatchers.Default)

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "native_coins_lib")
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
            "getPublicKeyHex" -> {
                getPrivateKeyHex(call, result)
            }
            "getPrivateKeyHex" -> {
                getPublicKeyHex(call, result)
            }
            "signTransaction" -> {
                signTransaction(call, result)
            }
            "ethSignHash2Rvs" -> {
                signHash2Rvs(call, result)
            }
            else -> result.notImplemented()
        }
    }

    private fun getPrivateKeyHex(call: MethodCall, result: Result) {
        val words = call.argument<List<String>>("words")
        val coinName = call.argument<String>("coinName")
        val passPhrase = call.argument<String>("passPhrase")
        try {
            if (null == words || null == coinName) {
                result.error("0", "words and coinName can not be null", "getPrivateKeyHex")
            } else {
                val wallet = walletFactory.getCoinWallet(CoinType.valueOf(coinName))
                result.success(wallet.getPrivateKey(words, passPhrase ?: ""))
            }
        } catch (e: Exception) {
            result.error("0", e.message, "getPrivateKeyHex")
            e.printStackTrace()
        }
    }

    private fun getPublicKeyHex(call: MethodCall, result: Result) {
        val words = call.argument<List<String>>("words")
        val coinName = call.argument<String>("coinName")
        val passPhrase = call.argument<String>("passPhrase")
        try {
            if (null == words || null == coinName) {
                result.error("0", "words and coinName can not be null", "getPublicKeyHex")
            } else {
                val wallet = walletFactory.getCoinWallet(CoinType.valueOf(coinName))
                result.success(wallet.getPublicKey(words, passPhrase ?: ""))
            }
        } catch (e: Exception) {
            result.error("0", e.message, "getPublicKeyHex")
            e.printStackTrace()
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
        if (null == words || null == inputTransaction || null == coinName) {
            result.error("0", "words and coinName and addressTo and inputTransaction can not be null", "signTransaction")
        } else {
            val coinWallet = walletFactory.getCoinWallet(CoinType.valueOf(coinName))
            pluginScope.launch(Dispatchers.IO) {
                try {
                    val signHash = coinWallet.signTransaction(inputTransaction, words, passPhrase ?: "")
                    withContext(Dispatchers.Main) {
                        result.success(signHash)
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        result.error("0", e.message, "signTransaction")
                    }
                }
            }
        }
    }

    private fun signHash2Rvs(call: MethodCall, result: Result) {
        val words = call.argument<List<String>>("words")
        val signHash = call.argument<String>("signHash")
        val passPhrase = call.argument<String>("passPhrase")
        val ethWallet = walletFactory.getCoinWallet(CoinType.ETH) as ETHWalletImp
        if (null == words || null == signHash || null == passPhrase) {
            result.error("0", "words and signHash and passPhrase can not be null", "signHash2Rvs")
        } else {
            pluginScope.launch(Dispatchers.IO) {
                try {
                    val signHash2Rvs = ethWallet.signHash2Rvs(hash = signHash, mnemonicWords = words, passPhrase = passPhrase)
                    withContext(Dispatchers.Main) {
                        result.success(signHash2Rvs);
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        result.error("0", e.message, "signHash2Rvs")
                    }
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
                result.success(coinWallet.checkAddress(address))
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
