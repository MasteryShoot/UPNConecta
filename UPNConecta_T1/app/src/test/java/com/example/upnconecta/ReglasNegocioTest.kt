package com.example.upnconecta

import com.example.upnconecta.data.Moderacion
import com.example.upnconecta.data.Roles
import com.example.upnconecta.data.sinTildes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** Pruebas de las reglas de negocio RN01, RN02, RN06 y RN09. */
class ReglasNegocioTest {

    @Test
    fun correoDeAlumnoAsignaRolAlumno() {
        assertEquals(Roles.ALUMNO, Roles.rolDeCorreo("N00329320@upn.pe"))
    }

    @Test
    fun correoDeDocenteAsignaRolDocente() {
        assertEquals(Roles.DOCENTE, Roles.rolDeCorreo("gerardo.sarmiento@upn.pe"))
    }

    @Test
    fun correoNoInstitucionalEsRechazado() {
        assertNull(Roles.rolDeCorreo("jose@gmail.com"))
    }

    @Test
    fun comentarioCortoEsRechazado() {
        assertNotNull(Moderacion.validarComentario("Muy bueno", obligatorio = true))
    }

    @Test
    fun comentarioConDatosPersonalesEsRechazado() {
        val texto = "Escríbanme al 987654321 para contarles más sobre el estado de los baños del pabellón."
        assertNotNull(Moderacion.validarComentario(texto, obligatorio = true))
    }

    @Test
    fun comentarioValidoEsAceptado() {
        val texto = "Los laboratorios están bien equipados, pero faltan enchufes en algunas mesas del aula."
        assertNull(Moderacion.validarComentario(texto, obligatorio = true))
    }

    @Test
    fun busquedaIgnoraTildes() {
        assertEquals(true, "María García Paredes".sinTildes().contains("garcia"))
    }
}
