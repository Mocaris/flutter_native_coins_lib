package org.mocaris.coins.wallet.imp

import com.google.common.base.Joiner
import com.google.protobuf.ByteString

import org.web3j.crypto.MnemonicUtils

import org.bitcoinj.crypto.ChildNumber
import org.bitcoinj.crypto.DeterministicHierarchy
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.crypto.HDKeyDerivation
import org.bouncycastle.util.encoders.Hex
import org.mocaris.coins.wallet.CoinWallet
import org.mocaris.coins.wallet.SignTransactionException
import org.mocaris.coins.wallet.coin.CoinType
import org.spongycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey
import org.tron.common.crypto.ECKey
import org.tron.common.crypto.Sha256Hash
import org.tron.keystore.Credentials
import org.tron.common.utils.ByteArray
import org.tron.protos.Protocol
import java.lang.Exception
import org.tron.walletserver.WalletApi


class TRXWalletImp : CoinWallet() {

    private val TRON_HARDENED = ChildNumber(195, true) //195

    private val number = 0

    override fun coinType(): CoinType = CoinType.TRX

    fun getBip44Credentials(mnemonicWords: List<String>, passPhrase: String = ""): Credentials {
        val words = Joiner.on(" ").join(mnemonicWords)
        val seed = MnemonicUtils.generateSeed(words, passPhrase)
        val rootPrivateKey: DeterministicKey = HDKeyDerivation.createMasterPrivateKey(seed)
        val deterministicHierarchy = DeterministicHierarchy(rootPrivateKey)
        val path = mutableListOf<ChildNumber>(ChildNumber(44, true), TRON_HARDENED, ChildNumber.ZERO_HARDENED)
        val fourpath: DeterministicKey = deterministicHierarchy.get(path, true, true)
        val fourpathhd: DeterministicKey = HDKeyDerivation.deriveChildKey(fourpath, 0)
        val fivepathhd: DeterministicKey = HDKeyDerivation.deriveChildKey(fourpathhd, number)
        val privateKeyByte = fivepathhd.privKeyBytes
        val ecKey: ECKey = ECKey.fromPrivate(privateKeyByte)
        ecKey.toStringWithPrivate()
        return Credentials.create(ecKey)
    }

    override fun generateAddress(mnemonicWords: List<String>, passPhrase: String): String {
        return getBip44Credentials(mnemonicWords, passPhrase).address
    }

    override fun getPrivateKey(mnemonicWords: List<String>, passPhrase: String): String {
        return Hex.toHexString(getBip44Credentials(mnemonicWords, passPhrase).ecKeyPair.pubKey)
    }

    override fun getPublicKey(mnemonicWords: List<String>, passPhrase: String): String {
        return Hex.toHexString(getBip44Credentials(mnemonicWords, passPhrase).ecKeyPair.privKey.toByteArray())
    }

    override fun signTransaction(inputTransaction: String, mnemonicWords: List<String>, passPhrase: String): String {
        try {
            val bip44Credentials = getBip44Credentials(mnemonicWords, passPhrase)
            val transaction = Protocol.Transaction.parseFrom(ByteArray.fromHexString(inputTransaction))
            val rawdata = transaction.rawData.toByteArray()
            val hash = Sha256Hash.hash(rawdata)
            val sign = bip44Credentials.ecKeyPair.sign(hash).toByteArray()
            val bytes = transaction.toBuilder().addSignature(ByteString.copyFrom(sign)).build().toByteArray()
            return ByteArray.toHexString(bytes)
        } catch (e: Exception) {
            e.printStackTrace()
            throw SignTransactionException(e.message)
        }
    }

    override fun checkAddress(address: String): Boolean {
        if (address.length != 34 || address.contains("0x")) {
            return false
        }
        val bytes = WalletApi.decodeFromBase58Check(address)
        return WalletApi.addressValid(bytes)

    }
}