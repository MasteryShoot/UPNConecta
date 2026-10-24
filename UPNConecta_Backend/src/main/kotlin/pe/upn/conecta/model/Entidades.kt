package pe.upn.conecta.model

import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapKeyColumn
import jakarta.persistence.Table
import java.time.LocalDateTime

/*
 * Entidades JPA según el diagrama E-R de la S08.
 * Los nombres de columnas coinciden con V1__crear_tablas.sql.
 */

@Entity
@Table(name = "universidad")
class Universidad(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_universidad")
    var idUniversidad: Int? = null,
    var nombre: String = "",
    var siglas: String = "",
    @Column(name = "dominio_correo")
    var dominioCorreo: String = ""
)

@Entity
@Table(name = "sede")
class Sede(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sede")
    var idSede: Int? = null,
    @Column(name = "id_universidad")
    var idUniversidad: Int = 0,
    var codigo: String = "",
    var nombre: String = "",
    var direccion: String = ""
)

@Entity
@Table(name = "usuario")
class Usuario(
    @Id
    var codigo: String = "",
    @Column(name = "id_universidad")
    var idUniversidad: Int = 0,
    var correo: String = "",
    @Column(name = "nombre_completo")
    var nombreCompleto: String = "",
    var rol: String = "",
    var verificado: Boolean = false,
    // null cuando la cuenta se creó con Microsoft (no tiene contraseña propia)
    @Column(name = "clave_hash")
    var claveHash: String? = null,
    @Column(name = "sede_codigo")
    var sedeCodigo: String? = null,
    // CORREO = registro con contraseña, MICROSOFT = inicio con la cuenta institucional
    var proveedor: String = "CORREO"
)

@Entity
@Table(name = "criterio")
class Criterio(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_criterio")
    var idCriterio: Int? = null,
    var area: String = "",
    var nombre: String = "",
    var orden: Int = 0
)

/**
 * Reseña (evaluación) que un usuario hace de un área de una sede.
 * Las calificaciones por criterio se guardan en la tabla CALIFICACION
 * como un mapa idCriterio -> puntaje.
 */
@Entity
@Table(name = "resena")
class Resena(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_resena")
    var idResena: Int? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario")
    var usuario: Usuario? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_sede")
    var sede: Sede? = null,

    var area: String = "",
    var periodo: String = "",
    var experiencia: Int = 0,
    var comentario: String? = null,
    @Column(name = "es_anonima")
    var esAnonima: Boolean = false,
    @Column(name = "sin_reciclaje")
    var sinReciclaje: Boolean = false,
    var fecha: LocalDateTime = LocalDateTime.now(),

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "calificacion", joinColumns = [JoinColumn(name = "id_resena")])
    @MapKeyColumn(name = "id_criterio")
    @Column(name = "puntaje")
    var calificaciones: MutableMap<Int, Int> = mutableMapOf()
)
