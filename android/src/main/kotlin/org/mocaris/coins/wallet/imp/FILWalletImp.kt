package org.mocaris.coins.wallet.imp

import com.google.common.collect.ImmutableList
import org.bitcoinj.core.ECKey
import org.bitcoinj.crypto.ChildNumber
import org.bitcoinj.crypto.DeterministicHierarchy
import org.bitcoinj.crypto.HDKeyDerivation
import org.json.JSONObject
import org.mocaris.coins.wallet.imp.filecoin.Base32
import org.mocaris.coins.wallet.imp.filecoin.FilCoinTransaction
import org.mocaris.coins.wallet.imp.filecoin.FilcoinSign
import org.mocaris.coins.wallet.CoinWallet
import org.mocaris.coins.wallet.SignTransactionException
import org.mocaris.coins.wallet.coin.CoinType
import org.mocaris.coins.wallet.mnemonic.MnemonicUtil
import org.web3j.crypto.MnemonicUtils
import org.web3j.utils.Numeric
import ove.crypto.digest.Blake2b
import java.math.BigDecimal

class FILWalletImp : CoinWallet() {

    private val HARDENED = ChildNumber(461, true)

    private val number = 0

    override fun coinType(): CoinType = CoinType.FIL

    override fun generateAddress(mnemonicWords: List<String>, passPhrase: String): String {
        val ecKey = getBip44Credentials(mnemonicWords, passPhrase)
        val pulStr = ecKey.publicKeyAsHex
        val bytes = Numeric.hexStringToByteArray(pulStr)
        val black2HashByte = Blake2b.Digest.newInstance(20).digest(bytes)
        val black2HashStr = Numeric.toHexStringNoPrefix(black2HashByte)
        val black2HashSecond = "0x01$black2HashStr"
        val blake2b2 = Blake2b.Digest.newInstance(4)
        val checksumBytes = blake2b2.digest(Numeric.hexStringToByteArray(black2HashSecond))
        val addressBytes = ByteArray(black2HashByte.size + checksumBytes.size)
        System.arraycopy(black2HashByte, 0, addressBytes, 0, black2HashByte.size)
        System.arraycopy(checksumBytes, 0, addressBytes, black2HashByte.size, checksumBytes.size)
        //f 正式 t 测试 1 钱包 2 合约
        return "f1" + Base32.encode(addressBytes)

    }

    override fun signTransaction(inputTransaction: String,mnemonicWords: List<String>, passPhrase: String): String {
        try {
            val jsonObject: JSONObject = JSONObject(inputTransaction)
            val value: String = jsonObject.getString("value")
            val amount = BigDecimal(value).multiply(BigDecimal.TEN.pow(18))
            val tran = FilCoinTransaction(
                from = jsonObject.getString("from"),
                to = jsonObject.getString("to"),
                value = amount.toBigInteger().toString(),
                nonce = jsonObject.getLong("nonce"),
                gasLimit = jsonObject.getLong("gasLimit"),
                gasFeeCap = jsonObject.getString("gasFeeCap"),
                gasPremium = jsonObject.getString("gasPremium"),
                method = 0L
            )
            return FilcoinSign.signTransaction(tran, mnemonicWords)?.let { sign ->
                val cid = JSONObject()
                val signer = JSONObject()
                val message = JSONObject()
                val callback = JSONObject()
                val ncid = JSONObject()

                cid.put("/", "")
                ncid.put("/", "")

                signer.put("Type", 1)
                signer.put("Data", sign)

                message.put("To", tran.to)
                message.put("From", tran.from)
                message.put("Nonce", tran.nonce)
                message.put("Value", tran.value)
                message.put("GasLimit", tran.gasLimit)
                message.put("GasFeeCap", tran.gasFeeCap)
                message.put("GasPremium", tran.gasPremium)
                message.put("Method", 0)
                message.put("Params", "")
                message.put("CID", cid)

                callback.put("Message", message)
                callback.put("Signature", signer)
                callback.put("CID", ncid)
                callback.toString()
            } ?: throw SignTransactionException("签名失败")
        } catch (e: Exception) {
            e.printStackTrace()
            throw SignTransactionException(e.message)
        }
    }

    override fun checkAddress(address: String): Boolean {
        return address.startsWith("f1")
    }


    private fun getBip44Credentials(mnemonicWords: List<String>, passPhrase: String): ECKey {
        val words = MnemonicUtil.toArrayString(mnemonicWords)
        val seed = MnemonicUtils.generateSeed(words, passPhrase)
        val rootPrivateKey = HDKeyDerivation.createMasterPrivateKey(seed)
        val deterministicHierarchy = DeterministicHierarchy(rootPrivateKey)
        val path = ImmutableList.of(ChildNumber(44, true), HARDENED, ChildNumber.ZERO_HARDENED)
        val fourpath = deterministicHierarchy[path, true, true]
        val fourpathhd = HDKeyDerivation.deriveChildKey(fourpath, 0)
        val fivepathhd = HDKeyDerivation.deriveChildKey(fourpathhd, number)
        return ECKey.fromPrivate(fivepathhd.privKey, false)
    }


    override fun getPrivateKey(mnemonicWords: List<String>, passPhrase: String): String {
        return getBip44Credentials(mnemonicWords, passPhrase).privateKeyAsHex
    }

    override fun getPublicKey(mnemonicWords: List<String>, passPhrase: String): String {
        return getBip44Credentials(mnemonicWords, passPhrase).publicKeyAsHex
    }

}