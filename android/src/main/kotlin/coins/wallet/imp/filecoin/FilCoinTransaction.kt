package coins.wallet.imp.filecoin

/**
 * * @Creator  Jacky
 * * @CreateTime 2020/11/6
 * * @Description
 */
public data class FilCoinTransaction(
    var to: String = "",
    var from: String = "",
    var nonce: Long = 0,
    var value: String = "",
    var gasLimit: Long = 0,
    var gasFeeCap: String = "",
    var gasPremium: String = "",
    var method: Long = 0,
    var params: String = "",
    var version: Int = 0,
    var CID: String = "",
)