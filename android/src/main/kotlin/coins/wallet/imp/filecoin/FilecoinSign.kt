package coins.wallet.imp.filecoin

import co.nstant.`in`.cbor.CborBuilder
import co.nstant.`in`.cbor.CborEncoder
import co.nstant.`in`.cbor.CborException
import co.nstant.`in`.cbor.model.ByteString
import co.nstant.`in`.cbor.model.UnsignedInteger
import com.google.common.collect.ImmutableList
import org.bitcoinj.crypto.ChildNumber
import org.bitcoinj.crypto.DeterministicHierarchy
import org.bitcoinj.crypto.HDKeyDerivation
import coins.wallet.mnemonic.MnemonicUtil
import coins.wallet.security.Base64Java
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.MnemonicUtils
import org.web3j.crypto.Sign
import org.web3j.crypto.Sign.SignatureData
import ove.crypto.digest.Blake2b
import java.io.ByteArrayOutputStream
import java.math.BigInteger

/**
 * * @Creator  Jacky
 * * @CreateTime 2020/11/7
 * * @Description
 */
object FilecoinSign {

    val CID_PREFIX = byteArrayOf(0x01, 0x71, 0xa0.toByte(), 0xe4.toByte(), 0x02, 0x20)
    val FIL_HARDENED = ChildNumber(461, true)

    fun transaction(tran: FileCoinTransaction): SignData {
        //构建交易结构体
        val from = getByte(tran.from)
        val to = getByte(tran.to)
        val signData = SignData()
        signData.version = UnsignedInteger(0)
        signData.to = ByteString(to)
        signData.from = ByteString(from)
        signData.nonce = UnsignedInteger(tran.nonce)


        val valueByteString: ByteString = if (BigInteger(tran.value).toByteArray()[0] != 0.toByte()) {
            val byte1 = ByteArray(BigInteger(tran.value).toByteArray().size + 1)
            byte1[0] = 0
            System.arraycopy(BigInteger(tran.value).toByteArray(), 0, byte1, 1, BigInteger(tran.value).toByteArray().size)
            ByteString(byte1)
        } else {
            ByteString(BigInteger(tran.value).toByteArray())
        }
        signData.value = valueByteString
        signData.gasLimit = UnsignedInteger(tran.gasLimit)
        val gasFeeCapString: ByteString
        if (BigInteger(tran.gasFeeCap).toByteArray()[0] != 0.toByte()) {
            val byte2 = ByteArray(BigInteger(tran.gasFeeCap).toByteArray().size + 1)
            byte2[0] = 0
            System.arraycopy(
                BigInteger(tran.gasFeeCap).toByteArray(), 0, byte2, 1, BigInteger(tran.gasFeeCap).toByteArray().size
            )
            gasFeeCapString = ByteString(byte2)
        } else {
            gasFeeCapString = ByteString(BigInteger(tran.gasFeeCap).toByteArray())
        }
        signData.gasFeeCap = gasFeeCapString
        val gasGasPremium: ByteString
        if (BigInteger(tran.gasPremium).toByteArray()[0] != 0.toByte()) {
            val byte2 = ByteArray(BigInteger(tran.gasPremium).toByteArray().size + 1)
            byte2[0] = 0
            System.arraycopy(
                BigInteger(tran.gasPremium).toByteArray(), 0, byte2, 1, BigInteger(tran.gasPremium).toByteArray().size
            )
            gasGasPremium = ByteString(byte2)
        } else {
            gasGasPremium = ByteString(BigInteger(tran.gasPremium).toByteArray())
        }
        signData.gasPremium = gasGasPremium
        signData.methodNum = UnsignedInteger(0)
        signData.params = ByteString(ByteArray(0))
        return signData
    }

    @JvmStatic
    fun signTransaction(tran: FileCoinTransaction, mnemonicWords: List<String>): String? {
        val (version, from, to, nonce, value, gasFeeCap, gasPremium, gasLimit, methodNum) = transaction(tran)
        val baos = ByteArrayOutputStream()
        try {
            CborEncoder(baos).encode(
                CborBuilder()
                    .addArray()
                    .add(version)
                    .add(to)
                    .add(from)
                    .add(nonce)
                    .add(value)
                    .add(gasLimit)
                    .add(gasFeeCap)
                    .add(gasPremium)
                    .add(methodNum)
                    .add(ByteString(byteArrayOf()))
                    .end()
                    .build()
            )
            val encodedBytes = baos.toByteArray()
            val cidHashBytes = getCidHash(encodedBytes)
            return sign(cidHashBytes, mnemonicWords)
        } catch (e: CborException) {
            e.printStackTrace()
        }
        return null
    }

    private fun getCidHash(message: ByteArray?): ByteArray {
        val messageByte = Blake2b.Digest.newInstance(32).digest(message)
        val xlen = CID_PREFIX.size
        val ylen = messageByte.size
        val result = ByteArray(xlen + ylen)
        System.arraycopy(CID_PREFIX, 0, result, 0, xlen)
        System.arraycopy(messageByte, 0, result, xlen, ylen)
        //        String prefixByteHex = Numeric.toHexString(prefixByte).substring(2);
        return Blake2b.Digest.newInstance(32).digest(result)
    }

    private fun sign(cidHash: ByteArray?, mnemonicWords: List<String>): String {
        val words = MnemonicUtil.toArrayString(mnemonicWords)
        val seed = MnemonicUtils.generateSeed(words, "")
        val rootPrivateKey = HDKeyDerivation.createMasterPrivateKey(seed)
        val deterministicHierarchy = DeterministicHierarchy(rootPrivateKey)
        val path = ImmutableList.of(ChildNumber(44, true), FIL_HARDENED, ChildNumber.ZERO_HARDENED)
        val fourpath = deterministicHierarchy[path, true, true]
        val fourpathhd = HDKeyDerivation.deriveChildKey(fourpath, 0)
        val fivepathhd = HDKeyDerivation.deriveChildKey(fourpathhd, 0)
        val ecKeyPair = ECKeyPair.create(fivepathhd.privKeyBytes)
        val signatureData = Sign.signMessage(cidHash, ecKeyPair, false)
        val sig = getSignature(signatureData)
        //        String stringHex = Numeric.toHexString(sig).substring(2);
        return Base64Java.getEncoder().encodeToString(sig)
    }

    private fun getSignature(signatureData: SignatureData): ByteArray {
        val sig = ByteArray(65)
        System.arraycopy(signatureData.r, 0, sig, 0, 32)
        System.arraycopy(signatureData.s, 0, sig, 32, 32)
        sig[64] = ((signatureData.v[0] and 0xFF.toByte()) - 27).toByte()
        return sig
    }

    private fun getByte(addressStr: String): ByteArray {
        val str = addressStr.substring(2)
        val bytes12 = ByteArray(21)
        bytes12[0] = 1
        System.arraycopy(Base32.decode(str), 0, bytes12, 1, 20)
        return bytes12
    }
}