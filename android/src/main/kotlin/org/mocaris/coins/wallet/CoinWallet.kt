package org.mocaris.coins.wallet

import org.mocaris.coins.wallet.coin.CoinType


/**
 * @Author mocaris
 * @Date 2019-11-04 11:33
 */
abstract class CoinWallet {

    class Config(var testNet: Boolean = false)

    companion object {

        /**
         * eos 账号规则
         */
        const val EOS_ACCOUNTREG = "[a-zA-Z1-5]+"

        /**
         * 配置
         */
        val config = Config()
    }

    abstract fun coinType(): CoinType

    /**
     * 钱包地址
     * generateReceive address by mnemonicWords
     *
     *  [mnemonicWords] 助记词
     *  [passPhrase]    加密 密码
     */
    abstract fun generateAddress(mnemonicWords: List<String>, passPhrase: String = ""): String

    /**
     * 私钥
     */
    abstract fun getPrivateKey(mnemonicWords: List<String>, passPhrase: String): String

    /**
     * 公钥
     */
    abstract fun getPublicKey(mnemonicWords: List<String>, passPhrase: String): String

    /**
     * 交易签名
     * signature for  coin trading
     *
     * [inputTransaction]  交易信息
     *  [addr]             付款方地址
     *  [mnemonicWords]    助记词
     *  [passPhrase]       密码
     */
    @Throws(SignTransactionException::class)
    abstract fun signTransaction(
        inputTransaction: String,
        addr: String,
        mnemonicWords: List<String>,
        passPhrase: String = ""
    ): String


    /**
     * 检测地址正确性
     * [address] 钱包地址
     */
    abstract fun checkAddress(address: String): Boolean


}