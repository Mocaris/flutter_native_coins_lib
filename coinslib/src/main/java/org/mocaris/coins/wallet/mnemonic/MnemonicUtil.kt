package org.mocaris.coins.wallet.mnemonic

import org.bitcoinj.crypto.MnemonicCode
import org.bitcoinj.wallet.DeterministicSeed
import java.security.SecureRandom


/**
 * @Author mocaris
 * @Date 2019-11-04 15:59
 */
object MnemonicUtil {

    /**
     * 检查助记词的正确性
     */
    fun checkMnemonic(words: List<String>): Boolean {
        return try {
            MnemonicCode.INSTANCE.check(words)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun checkMnemonic(words: String): Boolean {
        val toList = toList(words)
        return checkMnemonic(toList)
    }

    fun toList(mnemonic: String): List<String> {
        return mnemonic.split(Regex(" "))
    }

    fun toArrayString(mnemonic: List<String>): String {
        return mnemonic.joinToString(" ")
    }

    fun getAllMnemonicWord(): MutableList<String> {
        return MnemonicCode.INSTANCE.wordList
    }

    fun matchMnemonicWord(word: String): List<String> {
        return getAllMnemonicWord().filter {
            it.startsWith(word, false)
        }
    }

    /**
     * 创建助记词
     */
    fun createMnemonicWord(): List<String> {
        return try {
            getMnemonicList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    //获取种子
    private fun getEntropy(random: SecureRandom): ByteArray {
        val bits = DeterministicSeed.DEFAULT_SEED_ENTROPY_BITS
        val seed = ByteArray(bits / 8)
        random.nextBytes(seed)
        return seed
    }

    //根据种子获得助记词
    private fun getMnemonicList(): List<String> {
        val entropy = getEntropy(SecureRandom())
        return MnemonicCode.INSTANCE.toMnemonic(entropy)
    }

}