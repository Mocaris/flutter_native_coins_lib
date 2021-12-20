package org.mocaris.coins.wallet.imp.filecoin

import co.nstant.`in`.cbor.model.ByteString
import co.nstant.`in`.cbor.model.UnsignedInteger


/**
 * * @Creator  Jacky
 * * @CreateTime 2020/11/6
 * * @Description
 */
public  data class FilSignData(
    var version: UnsignedInteger? = null,
    var from: ByteString? = null,
    var to: ByteString? = null,
    var nonce: UnsignedInteger? = null,
    var value: ByteString? = null,
    var gasFeeCap: ByteString? = null,
    var gasPremium: ByteString? = null,
    var gasLimit: UnsignedInteger? = null,
    var methodNum: UnsignedInteger? = null,
    //空数组
    var params: ByteString? = null
)