package org.mocaris.coins.wallet.imp

import bch.org.bitcoinj.core.*
import bch.org.bitcoinj.core.Utils.HEX
import bch.org.bitcoinj.crypto.ChildNumber
import bch.org.bitcoinj.crypto.DeterministicHierarchy
import bch.org.bitcoinj.crypto.HDKeyDerivation
import bch.org.bitcoinj.crypto.MnemonicCode
import bch.org.bitcoinj.params.MainNetParams
import bch.org.bitcoinj.params.TestNet3Params
import bch.org.bitcoinj.script.ScriptBuilder
import bch.org.bitcoinj.wallet.DeterministicSeed
import bch.org.bitcoinj.wallet.KeyChain
import bch.org.bitcoinj.wallet.Wallet
import com.google.common.primitives.Longs
import org.mocaris.coins.wallet.CoinWallet
import org.mocaris.coins.wallet.SignTransactionException
import org.mocaris.coins.wallet.coin.CoinType


/**
 * @Author mocaris
 * @Date 2019-11-04 13:47
 */
class BCHWalletImp : CoinWallet() {

    private val HARDENED = ChildNumber(145, true)

    private val netParam = if (config.testNet) TestNet3Params.get() else MainNetParams.get()

    private val number = 0

    private fun getWallet(mnemonicWords: List<String>, passPhrase: String): Wallet {
        val seeded = MnemonicCode.toSeed(mnemonicWords, passPhrase)
        val seed = DeterministicSeed(mnemonicWords, seeded, passPhrase, Utils.currentTimeSeconds())
        return Wallet.fromSeed(netParam, seed)
    }

    private fun generateBip32Address(wallet: Wallet): String {
        wallet.freshKeys(KeyChain.KeyPurpose.RECEIVE_FUNDS, number)
        val addressList = wallet.issuedReceiveAddresses
        return addressList[addressList.size - 1].toString()
    }

    private fun createBip32Eckey(mnemonicWords: List<String>, passPhrase: String): ECKey? {
        val wallet = getWallet(mnemonicWords, passPhrase)
        val bip32Address = generateBip32Address(wallet)
        val scriptPubKey =
            ScriptBuilder.createOutputScript(Address.fromBase58(netParam, bip32Address))
        val pubKeyHash = scriptPubKey.pubKeyHash
        val ecKey = wallet.findKeyFromPubHash(pubKeyHash)
        return ecKey
    }

    private fun createBip44ECKey(mnemonicWords: List<String>, passPhrase: String): ECKey {
        val seeded = MnemonicCode.toSeed(mnemonicWords, passPhrase)
        val seed = DeterministicSeed(mnemonicWords, seeded, "", Utils.currentTimeSeconds())
        val rootPrivateKey = HDKeyDerivation.createMasterPrivateKey(seed.seedBytes!!)
        val deterministicHierarchy = DeterministicHierarchy(rootPrivateKey)
        val path = listOf(ChildNumber(44, true), HARDENED, ChildNumber.ZERO_HARDENED)
        val fourpath = deterministicHierarchy.get(path, true, true)
        val fourpathhd = HDKeyDerivation.deriveChildKey(fourpath, 0)
        val fivepathhd = HDKeyDerivation.deriveChildKey(fourpathhd, number)
        return ECKey.fromPrivate(fivepathhd.privKey)
    }

    private fun generateBip44Address(mnemonicWords: List<String>, passPhrase: String): String {
        val ecKey = createBip44ECKey(mnemonicWords, passPhrase)
        return ecKey.toAddress(netParam).toBase58()
    }

    override fun coinType(): CoinType {
        return CoinType.BCH
    }

    override fun generateAddress(mnemonicWords: List<String>, passPhrase: String): String {
        return generateBip44Address(mnemonicWords, passPhrase)
    }

    override fun getPrivateKey(mnemonicWords: List<String>, passPhrase: String): String {
        return createBip44ECKey(mnemonicWords,passPhrase).privateKeyAsHex
    }

    override fun getPublicKey(mnemonicWords: List<String>, passPhrase: String): String {
        return createBip44ECKey(mnemonicWords,passPhrase).publicKeyAsHex
    }

    override fun signTransaction(
        inputTransaction: String,
        mnemonicWords: List<String>,
        passPhrase: String
    ): String {
        try {
            val bytes = HEX.decode(inputTransaction)
            val transaction = Transaction(netParam, bytes)
            val ecKey = createBip44ECKey(mnemonicWords, passPhrase)
            val address = ecKey.toAddress(netParam)
            val numInputs = transaction.inputs.size
            for (i in 0 until numInputs) {
                val txIn = transaction.getInput(i.toLong())
                // chunk DUP内 借用DUP的data来存放UTXO的金额
                val scriptChunk = txIn.scriptSig.chunks[0]
                val datas = scriptChunk.data
                if (datas == null || datas.size != 8) {
                    throw Exception("签名失败! \n错误信息: 待签名信息错误！")
                }
                val amount = Longs.fromByteArray(datas)
                if (amount <= 0) {
                    throw Exception("签名失败! \n错误信息: 交易金额应为正数！")
                }
                val scriptPubKey = ScriptBuilder.createOutputScript(address)
                val ecKey = createBip44ECKey(mnemonicWords, passPhrase)
                txIn.scriptSig = scriptPubKey.createEmptyInputScript(ecKey, scriptPubKey)
                var inputScript = txIn.scriptSig
                // 应该是byte类型的上笔交易的输出脚本
                val script = scriptPubKey.program
                val outputCoin = Coin.valueOf(amount)
                // BCH 与其他币的不同之处
                val signature = transaction.calculateWitnessSignature(
                    i,
                    ecKey,
                    script,
                    outputCoin,
                    Transaction.SigHash.ALL,
                    false
                )
                val sigIndex = 0
                inputScript = scriptPubKey.getScriptSigWithSignature(
                    inputScript,
                    signature.encodeToBitcoin(),
                    sigIndex
                )
                txIn.scriptSig = inputScript
            }
            // 已签名的交易对象序列化
            val spendBytes = transaction.bitcoinSerialize()
            return HEX.encode(spendBytes)
        } catch (e: Exception) {
            e.printStackTrace()
            throw SignTransactionException(e.message)
        }
    }

    override fun checkAddress(address: String): Boolean {
        return try {
            Address.fromBase58(netParam, address)
            true
        } catch (e: AddressFormatException) {
            false
        }
    }
}
