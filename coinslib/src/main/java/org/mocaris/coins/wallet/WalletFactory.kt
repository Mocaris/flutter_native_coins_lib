package org.mocaris.coins.wallet

import org.mocaris.coins.wallet.coin.CoinType
import org.mocaris.coins.wallet.imp.*


/**
 * @Author mocaris
 * @Date 2019-11-04 16:37
 */
class WalletFactory {

    private val walletCache = HashMap<CoinType, CoinWallet>()

    fun getCoinWallet(coinType: CoinType): CoinWallet {
        return walletCache[coinType] ?: let {
            val wallet: CoinWallet = when (coinType) {
                CoinType.BTC -> BTCWalletImp()
                CoinType.BCH -> BCHWalletImp()
                CoinType.ETH -> ETHWalletImp()
                CoinType.LTC -> LTCWalletImp()
                CoinType.DASH -> DASHWalletImp()
                CoinType.EOS -> EOSWalletImp()
                CoinType.ETC -> ETCWalletImp()
                CoinType.FIL -> FILWalletImp()
                CoinType.TRX -> TRXWalletImp()
            }
            walletCache[coinType] = wallet
            wallet
        }
    }

}