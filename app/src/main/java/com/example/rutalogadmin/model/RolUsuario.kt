package com.example.rutalogadmin.model

/** Valor que se guarda en la columna "rol" de la tabla usuarios. */
enum class RolUsuario(val valor: String, val etiqueta: String) {
    CLIENTE("cliente", "Cliente"),
    OPERADOR("operador", "Operador"),
    ADMINISTRADOR("administrador", "Administrador"),
    COURIER("courier", "Courier");

    companion object {
        /** Roles que entran a la App Admin. Aprobar liquidaciones y Mi crédito quedan para el administrador. */
        val deLaAppAdmin = listOf(OPERADOR, ADMINISTRADOR)

        fun desde(valor: String): RolUsuario? = entries.firstOrNull { it.valor == valor }
    }
}
