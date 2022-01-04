package org.mocaris.coins.wallet.imp

import com.google.common.base.Joiner
import org.bitcoinj.crypto.ChildNumber
import org.bitcoinj.crypto.DeterministicHierarchy
import org.bitcoinj.crypto.HDKeyDerivation
import org.json.JSONObject
import org.mocaris.coins.wallet.CoinWallet
import org.mocaris.coins.wallet.SignTransactionException
import org.mocaris.coins.wallet.coin.CoinType
import org.mocaris.coins.wallet.util.Utils
import org.web3j.crypto.*
import org.web3j.utils.Numeric


/**
 * @Author mocaris
 * @Date 2019-11-04 15:12
 */
open class ETHWalletImp : CoinWallet() {

    private val HARDENED = ChildNumber(60, true)

    private val number = 0

    private val chainId: Long = if (config.testNet) 4 else 1

    override fun coinType(): CoinType = CoinType.ETH

    private fun createBip39Credentials(
        mnemonicWords: List<String>,
        passPhrase: String
    ): Credentials {
        val words = Joiner.on(" ").join(mnemonicWords)
        val seed = MnemonicUtils.generateSeed(words, passPhrase)
        val ecKeyPair = ECKeyPair.create(Hash.sha256(seed))
        return Credentials.create(ecKeyPair)
    }

    fun generateBip39Address(credentials: Credentials): String {
        return credentials.address
    }

    private fun createBip44Credentials(
        mnemonicWords: List<String>,
        passPhrase: String
    ): Credentials {
        val words = Joiner.on(" ").join(mnemonicWords)
        val seed = MnemonicUtils.generateSeed(words, passPhrase)
        val rootPrivateKey = HDKeyDerivation.createMasterPrivateKey(seed)
        val deterministicHierarchy = DeterministicHierarchy(rootPrivateKey)
        val path = listOf(ChildNumber(44, true), HARDENED, ChildNumber.ZERO_HARDENED)
        val fourpath = deterministicHierarchy.get(path, true, true)
        val fourpathhd = HDKeyDerivation.deriveChildKey(fourpath, 0)
        val fivepathhd = HDKeyDerivation.deriveChildKey(fourpathhd, number)
        val privateKeyByte = fivepathhd.privKeyBytes
        val keyPair = ECKeyPair.create(privateKeyByte)
        return Credentials.create(keyPair)
    }

    private fun generateBip44Address(credentials: Credentials): String {
        return credentials.address
    }

    override fun generateAddress(mnemonicWords: List<String>, passPhrase: String): String {
        val credentials = createBip44Credentials(mnemonicWords, passPhrase)
        return generateBip44Address(credentials)
    }

    override fun signTransaction(
        inputTransaction: String,
        addr: String,
        mnemonicWords: List<String>,
        passPhrase: String
    ): String {
        try {
            val credentials = createBip44Credentials(mnemonicWords, passPhrase)
            val jsonObject = JSONObject(inputTransaction)
            val nonce = jsonObject.getString("nonce").toBigInteger()
            val gasPrice = jsonObject.getString("gasPrice").toBigInteger()
            val gasLimit = jsonObject.getString("gasLimit").toBigInteger()
            val value = jsonObject.getString("value").toBigInteger()
            val data = jsonObject.getString("data")
            val to = jsonObject.getString("to")
            val rawTransaction =
                RawTransaction.createTransaction(nonce, gasPrice, gasLimit, to, value, data)
            val signMessage = TransactionEncoder.signMessage(rawTransaction, chainId, credentials)
            return Numeric.toHexString(signMessage).substring(2)
        } catch (e: Exception) {
            e.printStackTrace()
            throw SignTransactionException(e.message)
        }
    }

    override fun checkAddress(address: String): Boolean {
        return WalletUtils.isValidAddress(address)
    }

    /**
     * 签名hash  输出 rsv 格式签名
     */
    fun signHash2Rvs(hash: String, mnemonicWords: List<String>, passPhrase: String = ""): String {
        try {
            val credentials = createBip44Credentials(mnemonicWords, passPhrase)
            val messageHash = Numeric.hexStringToByteArray(hash)
            val signMessage = Sign.signMessage(messageHash, credentials.ecKeyPair, false)
            return StringBuilder(Numeric.toHexString(signMessage.r))
                .append(Numeric.cleanHexPrefix(Numeric.toHexString(signMessage.s)))
                .append(Utils.byteToHex(signMessage.v[0]))
                .toString()
        } catch (e: Exception) {
            e.printStackTrace()
            throw SignTransactionException(e.message)
        }
    }

    /*
     * hash transation 数据
     */
    fun signTransation2RVS(
        inputTransaction: String,
        mnemonicWords: List<String>,
        passPhrase: String = ""
    ): String {
        try {
            val signHash = getSignHash(inputTransaction)
            return signHash2Rvs(signHash, mnemonicWords, passPhrase)
            /* val jsonObject = JSONObject(inputTransaction)
             val nonce = jsonObject.getString("nonce").toBigInteger()
             val gasPrice = jsonObject.getString("gasPrice").toBigInteger()
             val gasLimit = jsonObject.getString("gasLimit").toBigInteger()
             val value = jsonObject.getString("value").toBigInteger()
             val data = jsonObject.getString("data")
             val to = jsonObject.getString("to")
             val rawTransaction = RawTransaction.createTransaction(nonce, gasPrice, gasLimit, to, value, data)
             val encodedTransaction = TransactionEncoder.encode(rawTransaction)
             val messageHash = Hash.sha3(encodedTransaction)
             val credentials = createBip44Credentials(mnemonicWords, passPhrase)
             val signMessage = Sign.signMessage(messageHash, credentials.ecKeyPair,false)
             return Numeric.toHexString(signMessage.r) + Numeric.cleanHexPrefix(Numeric.toHexString(signMessage.s)) + Utils.byteToHex(signMessage.v)
         */
        } catch (e: Exception) {
            e.printStackTrace()
            throw SignTransactionException(e.message)
        }
    }

    private fun getSignHash(rawTransaction: String): String {
        try {
            val jsonObject = JSONObject(rawTransaction)
            val nonce = jsonObject.getString("nonce").toBigInteger()
            val gasPrice = jsonObject.getString("gasPrice").toBigInteger()
            val gasLimit = jsonObject.getString("gasLimit").toBigInteger()
            val value = jsonObject.getString("value").toBigInteger()
            val data = jsonObject.getString("data")
            val to = jsonObject.getString("to")
            val rawTransaction =
                RawTransaction.createTransaction(nonce, gasPrice, gasLimit, to, value, data)
            val encodedTransaction = TransactionEncoder.encode(rawTransaction)
            val messageHash = Hash.sha3(encodedTransaction)
            return Numeric.toHexString(messageHash)
        } catch (e: Exception) {
            e.printStackTrace()
            throw SignTransactionException(e.message)
        }
    }

}
