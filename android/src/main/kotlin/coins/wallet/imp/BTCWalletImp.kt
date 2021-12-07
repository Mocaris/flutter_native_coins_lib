package coins.wallet.imp

import org.bitcoinj.core.*
import org.bitcoinj.core.Utils.HEX
import org.bitcoinj.crypto.ChildNumber
import org.bitcoinj.crypto.DeterministicHierarchy
import org.bitcoinj.crypto.HDKeyDerivation
import org.bitcoinj.crypto.MnemonicCode
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.params.TestNet3Params
import org.bitcoinj.script.Script
import org.bitcoinj.script.ScriptBuilder
import org.bitcoinj.wallet.DeterministicSeed
import org.bitcoinj.wallet.KeyChain
import org.bitcoinj.wallet.Wallet
import coins.wallet.CoinWallet
import coins.wallet.SignTransactionException
import coins.wallet.coin.CoinType


/**
 * @Author mocaris
 * @Date 2019-11-04 11:41
 */

class BTCWalletImp : CoinWallet() {
    private val netParams = if (config.testNet) TestNet3Params.get() else MainNetParams.get()

    private val HARDENED = ChildNumber(0, true)

    private val number = 0

    private fun getWallet(mnemonicWords: List<String>, passPhrase: String): Wallet {
        val seeded = MnemonicCode.toSeed(mnemonicWords, passPhrase)
        val seed = DeterministicSeed(mnemonicWords, seeded, passPhrase, Utils.currentTimeSeconds())
        return Wallet.fromSeed(netParams, seed, Script.ScriptType.P2PKH)
    }

    override fun coinType(): CoinType {
        return CoinType.BTC
    }

    private fun createBip44ECKey(mnemonicWords: List<String>, passPhrase: String): ECKey {
        val seeded = MnemonicCode.toSeed(mnemonicWords, passPhrase)
        val seed = DeterministicSeed(mnemonicWords, seeded, "", Utils.currentTimeSeconds())
        val rootPrivateKey = HDKeyDerivation.createMasterPrivateKey(seed.seedBytes!!)
        val deterministicHierarchy = DeterministicHierarchy(rootPrivateKey)
        val path = mutableListOf(ChildNumber(44, true), HARDENED, ChildNumber.ZERO_HARDENED)
        val fourPath = deterministicHierarchy.get(path, true, true)
        val fourPathhd = HDKeyDerivation.deriveChildKey(fourPath, 0)
        val fivePathhd = HDKeyDerivation.deriveChildKey(fourPathhd, number)
        return ECKey.fromPrivate(fivePathhd.privKey)
    }

    private fun generateBip44Address(mnemonicWords: List<String>, passPhrase: String): String {
        val bip44ECKey = createBip44ECKey(mnemonicWords, passPhrase)
        return Address.fromKey(netParams, bip44ECKey, Script.ScriptType.P2PKH).toString()
    }

    private fun generateBip32Address(mnemonicWords: List<String>, passPhrase: String): String {
        val wallet = getWallet(mnemonicWords, passPhrase)
        wallet.freshKeys(KeyChain.KeyPurpose.RECEIVE_FUNDS, number)
        val addressList = wallet.issuedReceiveAddresses
        return addressList[addressList.size - 1].toString()
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
            val bytes = HEX.decode(inputTransaction)
            val transaction = Transaction(netParams, bytes)
            val ecKey = createBip44ECKey(mnemonicWords, passPhrase)
            val address = Address.fromKey(netParams, ecKey, Script.ScriptType.P2PKH)
            val scriptPubKey = ScriptBuilder.createOutputScript(address)
            // 对输入进行签名
            val numInputs = transaction.inputs.size
            for (i in 0 until numInputs) {
                val txIn = transaction.getInput(i.toLong())
                txIn.scriptSig = scriptPubKey.createEmptyInputScript(ecKey, scriptPubKey)
                var inputScript = txIn.scriptSig
                // 应该是byte类型的上笔交易的输出脚本
                val script = scriptPubKey.program
                val signature =
                    transaction.calculateSignature(i, ecKey, script, Transaction.SigHash.ALL, false)
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
            Address.fromString(netParams, address)
            true
        } catch (e: AddressFormatException) {
            false
        }
    }

}
