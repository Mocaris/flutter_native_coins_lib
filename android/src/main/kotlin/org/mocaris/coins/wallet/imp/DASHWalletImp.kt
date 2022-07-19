package org.mocaris.coins.wallet.imp

import dash.org.bitcoinj.core.*
import dash.org.bitcoinj.core.Utils.HEX
import dash.org.bitcoinj.crypto.ChildNumber
import dash.org.bitcoinj.crypto.DeterministicHierarchy
import dash.org.bitcoinj.crypto.HDKeyDerivation
import dash.org.bitcoinj.crypto.MnemonicCode
import dash.org.bitcoinj.params.MainNetParams
import dash.org.bitcoinj.params.TestNet3Params
import dash.org.bitcoinj.script.ScriptBuilder
import dash.org.bitcoinj.wallet.DeterministicSeed
import dash.org.bitcoinj.wallet.KeyChain
import dash.org.bitcoinj.wallet.Wallet
import org.mocaris.coins.wallet.CoinWallet
import org.mocaris.coins.wallet.SignTransactionException
import org.mocaris.coins.wallet.coin.CoinType


/**
 * @Author mocaris
 * @Date 2019-11-04 14:30
 */
class DASHWalletImp : CoinWallet() {

    private var HARDENED = ChildNumber(5, true)

    private var netParam = if (config.testNet) TestNet3Params.get() else MainNetParams.get()

    private var number = 0

    private fun getWallet(mnemonicWords: List<String>, passPhrase: String): Wallet {
        val seeded = MnemonicCode.toSeed(mnemonicWords, passPhrase)
        val seed = DeterministicSeed(mnemonicWords, seeded, "", Utils.currentTimeSeconds())
        return Wallet.fromSeed(netParam, seed)
    }

    private fun generateBip32Address(wallet: Wallet): String {
        wallet.freshKeys(KeyChain.KeyPurpose.RECEIVE_FUNDS, number)
        val addressList = wallet.issuedReceiveAddresses
        return addressList[addressList.size - 1].toString()
    }

    private fun createBip32Eckey(mnemonicWords: List<String>, passPhrase: String): ECKey? {
        val wallet = getWallet(mnemonicWords, passPhrase)
        val address = generateBip32Address(wallet)
        val scriptPubKey = ScriptBuilder.createOutputScript(Address.fromBase58(netParam, address))
        val pubKeyHash = scriptPubKey.getPubKeyHash();
        return wallet.findKeyFromPubHash(pubKeyHash);
    }


    private fun generateBip44Address(mnemonicWords: List<String>, passPhrase: String): String {
        val bip44ECKey = createBip44ECKey(mnemonicWords, passPhrase)
        val address = bip44ECKey.toAddress(netParam)
        return address.toBase58()
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

    override fun coinType(): CoinType = CoinType.DASH

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
            val scriptPubKey = ScriptBuilder.createOutputScript(address)
            // 对输入进行签名
            val numInputs = transaction.inputs.size
            for (i in 0 until numInputs) {
                val txIn = transaction.getInput(i.toLong())
                txIn.scriptSig = scriptPubKey.createEmptyInputScript(ecKey, scriptPubKey)
                var inputScript = txIn.scriptSig
                val script = scriptPubKey.getProgram()  // 应该是byte类型的上笔交易的输出脚本
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
            throw SignTransactionException(e.message)
        }
    }

    override fun checkAddress(address: String): Boolean {
        return try {
            Address.fromBase58(netParam, address) ?: return false
            true
        } catch (e: AddressFormatException) {
            false
        }
    }
}
