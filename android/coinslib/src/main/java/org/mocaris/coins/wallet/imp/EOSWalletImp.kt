package org.mocaris.coins.wallet.imp

import com.google.common.collect.ImmutableList
import io.eblock.eos4j.Ecc
import io.eblock.eos4j.OfflineSign
import io.eblock.eos4j.api.vo.SignParam
import io.eblock.eos4j.utils.Base58
import io.eblock.eos4j.utils.ByteUtils
import io.eblock.eos4j.utils.Sha
import org.bitcoinj.crypto.ChildNumber
import org.bitcoinj.crypto.DeterministicHierarchy
import org.bitcoinj.crypto.HDKeyDerivation
import org.json.JSONObject
import org.mocaris.coins.wallet.CoinWallet
import org.mocaris.coins.wallet.SignTransactionException
import org.mocaris.coins.wallet.coin.CoinType
import org.mocaris.coins.wallet.mnemonic.MnemonicUtil
import org.web3j.crypto.MnemonicUtils
import java.math.BigInteger
import java.util.*
import java.util.regex.Pattern

/**
 * @Author mocaris
 * @Date 2019-11-04 14:39
*/
class EOSWalletImp : CoinWallet() {

    private val HARDENED = ChildNumber(194, true)

    private val number = 0

    override fun coinType(): CoinType = CoinType.EOS

    private fun generateBip39Address(mnemonicWords: List<String>, passPhrase: String): String {
        val pk = getBip39pk(mnemonicWords, passPhrase)
        return Ecc.privateToPublic(pk)
    }

    private fun generateBip44Address(mnemonicWords: List<String>, passPhrase: String): String {
        val pk = getBip44pk(mnemonicWords, passPhrase)
        return Ecc.privateToPublic(pk)
    }


    private fun getBip39pk(mnemonicWords: List<String>, passPhrase: String): String {
        val words = MnemonicUtil.toArrayString(mnemonicWords)
        return Ecc.seedPrivate(words)
    }

    private fun getBip44pk(mnemonicWords: List<String>, passPhrase: String): String {
        val words = MnemonicUtil.toArrayString(mnemonicWords)
        val seed = MnemonicUtils.generateSeed(words, passPhrase)
        val rootPrivateKey = HDKeyDerivation.createMasterPrivateKey(seed)
        val deterministicHierarchy = DeterministicHierarchy(rootPrivateKey)
        val path = ImmutableList.of(ChildNumber(44, true), HARDENED, ChildNumber.ZERO_HARDENED)
        val fourpath = deterministicHierarchy.get(path, true, true)
        val fourpathhd = HDKeyDerivation.deriveChildKey(fourpath, 0)
        val fivepathhd = HDKeyDerivation.deriveChildKey(fourpathhd, number)
        val privateKeyByte = fivepathhd.privKeyBytes
        val a = byteArrayOf(0x80.toByte())
        val b = BigInteger(privateKeyByte).toByteArray()
        val private_key = ByteUtils.concat(a, b)
        var checksum = Sha.SHA256(private_key)
        checksum = Sha.SHA256(checksum)
        val check = ByteUtils.copy(checksum, 0, 4)
        val pk = ByteUtils.concat(private_key, check)
        return Base58.encode(pk)
    }


    override fun generateAddress(mnemonicWords: List<String>, passPhrase: String): String {
        return generateBip44Address(mnemonicWords, passPhrase)
    }

    override fun signTransaction(
        inputTransaction: String,
        addr: String,
        mnemonicWords: List<String>,
        passPhrase: String
    ): String {
        try {
            val jsonObject = JSONObject(inputTransaction)
            val addrfrom = jsonObject.getString("addrfrom")
            val addrto = jsonObject.getString("to")
            val value = jsonObject.getString("value")
            val sign = OfflineSign()
            val signParamsJson = jsonObject.getJSONObject("offlineSignParams")
            val offlineSignParams = SignParam()
            offlineSignParams.chainId = signParamsJson.getString("chainId")
            offlineSignParams.exp = signParamsJson.getLong("exp")
            offlineSignParams.headBlockTime = Date(signParamsJson.getLong("headBlockTime"))
            offlineSignParams.lastIrreversibleBlockNum =
                signParamsJson.getLong("lastIrreversibleBlockNum")
            offlineSignParams.refBlockPrefix = signParamsJson.getLong("refBlockPrefix")
            return sign.transfer(
                offlineSignParams,
                getBip44pk(mnemonicWords, passPhrase),
                "eosio.token",
                addrfrom,
                addrto,
                "$value EOS",
                ""
            )
        } catch (e: Exception) {
            e.printStackTrace()
            throw SignTransactionException(e.message)
        }
    }

    override fun checkAddress(address: String): Boolean {
        return Pattern.compile(EOS_ACCOUNTREG).matcher(address).matches() && address.length == 12
    }

}
