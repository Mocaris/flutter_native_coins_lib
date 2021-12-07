package coins.wallet.imp

import org.litecoinj.core.Address
import org.litecoinj.core.ECKey
import org.litecoinj.core.Transaction
import org.litecoinj.core.Utils
import org.litecoinj.core.Utils.HEX
import org.litecoinj.crypto.ChildNumber
import org.litecoinj.crypto.DeterministicHierarchy
import org.litecoinj.crypto.HDKeyDerivation
import org.litecoinj.crypto.MnemonicCode
import org.litecoinj.params.MainNetParams
import org.litecoinj.params.TestNet3Params
import org.litecoinj.script.Script
import org.litecoinj.script.ScriptBuilder
import org.litecoinj.wallet.DeterministicSeed
import org.litecoinj.wallet.KeyChain
import org.litecoinj.wallet.Wallet
import coins.wallet.CoinWallet
import coins.wallet.SignTransactionException
import coins.wallet.coin.CoinType

/**
 * @Author mocaris
 * @Date 2019-11-04 15:28
 */

class LTCWalletImp : CoinWallet() {

    private val HARENED = ChildNumber(5, true)

    private val netParam = if (config.testNet) TestNet3Params.get() else MainNetParams.get()

    private val number = 0

    private fun getWallet(mnemonicWords: List<String>, passPhrase: String): Wallet {
        val seeded = MnemonicCode.toSeed(mnemonicWords, passPhrase)
        val seed = DeterministicSeed(mnemonicWords, seeded, passPhrase, Utils.currentTimeSeconds())
        return Wallet.fromSeed(netParam, seed, Script.ScriptType.P2PKH)
    }

    private fun createBip39Eckey(mnemonicWords: List<String>, passPhrase: String): ECKey? {
        val wallet = getWallet(mnemonicWords, passPhrase)
        val address = generateBip39Address(wallet)
        val scriptPubKey = ScriptBuilder.createOutputScript(Address.fromString(netParam, address))
        val pubKeyHash = scriptPubKey.pubKeyHash;
        return wallet.findKeyFromPubKeyHash(pubKeyHash, Script.ScriptType.P2PKH);
    }

    private fun generateBip39Address(wallet: Wallet): String {
        wallet.freshKeys(KeyChain.KeyPurpose.RECEIVE_FUNDS, number)
        val addressList = wallet.issuedReceiveAddresses
        return addressList[addressList.size - 1].toString()
    }

    private fun generateBip44Address(bip44EcKey: ECKey): String {
        return Address.fromKey(netParam, bip44EcKey, Script.ScriptType.P2PKH).toString()
    }

    private fun createBip44ECKey(mnemonicWords: List<String>, passPhrase: String): ECKey {
        val seeded = MnemonicCode.toSeed(mnemonicWords, passPhrase)
        val seed = DeterministicSeed(mnemonicWords, seeded, "", Utils.currentTimeSeconds())
        val rootPrivateKey = HDKeyDerivation.createMasterPrivateKey(seed.seedBytes!!)
        val deterministicHierarchy = DeterministicHierarchy(rootPrivateKey)
        val path = listOf(ChildNumber(44, true), HARENED, ChildNumber.ZERO_HARDENED)
        val fourpath = deterministicHierarchy.get(path, true, true)
        val fourpathhd = HDKeyDerivation.deriveChildKey(fourpath, 0)
        val fivepathhd = HDKeyDerivation.deriveChildKey(fourpathhd, number)
        return ECKey.fromPrivate(fivepathhd.privKey)
    }

    override fun coinType(): CoinType = CoinType.LTC

    override fun generateAddress(mnemonicWords: List<String>, passPhrase: String): String {
        val ecKey = createBip44ECKey(mnemonicWords, passPhrase)
        return generateBip44Address(ecKey)
    }

    override fun signTransaction(
        inputTransaction: String,
        addr: String,
        mnemonicWords: List<String>,
        passPhrase: String
    ): String {
        try {
            val bytes = HEX.decode(inputTransaction)
            val transaction = Transaction(netParam, bytes)
            // 对输入进行签名
            val numInputs = transaction.inputs.size
            val ecKey = createBip44ECKey(mnemonicWords, passPhrase)
            val address = Address.fromKey(netParam, ecKey, Script.ScriptType.P2PKH)
            val scriptPubKey = ScriptBuilder.createOutputScript(address)
            for (i in 0 until numInputs) {
                val txIn = transaction.getInput(i.toLong())
                txIn.scriptSig = scriptPubKey.createEmptyInputScript(ecKey, scriptPubKey)
                var inputScript = txIn.scriptSig
                // 应该是byte类型的上笔交易的输出脚本
                val script = scriptPubKey.program
                val signature =
                    transaction.calculateSignature(
                        i,
                        ecKey,
                        script,
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
            Address.fromString(netParam, address)
            true
        } catch (e: Exception) {
            false
        }
    }
}
