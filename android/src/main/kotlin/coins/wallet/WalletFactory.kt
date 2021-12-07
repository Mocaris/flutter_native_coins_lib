package coins.wallet

import coins.wallet.imp.BCHWalletImp
import coins.wallet.coin.CoinType
import coins.wallet.imp.BTCWalletImp
import coins.wallet.imp.*


/**
 * @Author mocaris
 * @Date 2019-11-04 16:37
 */
class WalletFactory {

    private val walletCache = HashMap<CoinType, CoinWallet>()

    /**
     * 不可以传入UNKNOWN
     */
    @Throws(RuntimeException::class)
    fun getCoinWallet(coinType: CoinType): CoinWallet {
        return walletCache[coinType] ?: let {
            val wallet = when (coinType) {
                CoinType.BTC -> BTCWalletImp()
                CoinType.BCH -> BCHWalletImp()
                CoinType.ETH -> ETHWalletImp()
                CoinType.LTC -> LTCWalletImp()
                CoinType.DASH -> DASHWalletImp()
                CoinType.EOS -> EOSWalletImp()
                CoinType.ETC -> ETCWalletImp()
                CoinType.FIL -> FILWalletImp()
                CoinType.TRX -> TRXWalletImp()
                else -> throw RuntimeException("coinType can not be unknown")
            }
            walletCache[coinType] = wallet
            wallet
        }
    }

}