package com.example.rutalogadmin

import com.example.rutalogadmin.viewmodel.PagosViewModel
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** RF-O06 / RN-07: reglas de la solicitud de crédito (las mismas que validan la API y el banco). */
class SolicitudCreditoTest {

    @Test
    fun solicitudValida_laDeLaFicha() {
        assertNull(PagosViewModel.validarSolicitud("70000", "48", autorizo = true))
        assertNull(PagosViewModel.validarSolicitud("70,000", "48", autorizo = true))
    }

    @Test
    fun sinAutorizacionNoSeEnviaNadaAlBanco() {
        assertNotNull(PagosViewModel.validarSolicitud("70000", "48", autorizo = false))
    }

    @Test
    fun montoYPlazoFueraDeRango() {
        assertNotNull(PagosViewModel.validarSolicitud("0", "48", autorizo = true))
        assertNotNull(PagosViewModel.validarSolicitud("abc", "48", autorizo = true))
        assertNotNull(PagosViewModel.validarSolicitud("2000000", "48", autorizo = true))
        assertNotNull(PagosViewModel.validarSolicitud("70000", "5", autorizo = true))
        assertNotNull(PagosViewModel.validarSolicitud("70000", "85", autorizo = true))
    }
}
