package com.pinguela.reflejovision.rest.api;

import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.luis.reflejovision.dao.DataException;
import com.luis.reflejovision.model.Results;
import com.luis.reflejovision.model.Usuario;
import com.luis.reflejovision.model.UsuarioCriteria;
import com.luis.reflejovision.service.ServiceException;
import com.luis.reflejovision.service.UsuarioService;
import com.luis.reflejovision.service.impl.UsuarioServiceImpl;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@Path("/usuario")
@Singleton
public class UsuarioResource {
	private UsuarioService usuarioService = null;
	private static Logger logger = LogManager.getLogger(UsuarioResource.class);

	public UsuarioResource() {
		usuarioService = new UsuarioServiceImpl();
	}

	@POST
	@Consumes("application/x-www-form-urlencoded") // Indica que los datos deben enviarse en formato formulario
	@Produces(MediaType.APPLICATION_JSON) // La respuesta será en formato JSON
	@Operation(summary = "Registrar un nuevo usuario", description = "Este endpoint permite registrar un nuevo usuario en el sistema. El usuario se crea con los parámetros proporcionados, incluyendo contraseña, nombre, rol, y correo.", responses = {
			@ApiResponse(responseCode = "200", description = "Usuario registrado exitosamente", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Usuario.class))),
			@ApiResponse(responseCode = "400", description = "Datos de entrada inválidos. Esto puede ocurrir si alguno de los parámetros no es válido, como una contraseña vacía o un correo mal formateado."),
			@ApiResponse(responseCode = "500", description = "Error interno en el servidor al registrar el usuario. Puede deberse a un problema con la base de datos o con la lógica del servicio.") })
	public Response registrar(@FormParam("contrasena") String contrasena, @FormParam("username") String username,
			@FormParam("nombre") String nombre, @FormParam("id_rol") Long id_rol, @FormParam("correo") String correo) {

		// Validación simple de los parámetros
		if (contrasena == null || contrasena.isEmpty() || username == null || username.isEmpty() || nombre == null
				|| nombre.isEmpty() || id_rol == null || correo == null || correo.isEmpty()) {
			return Response.status(Response.Status.BAD_REQUEST)
					.entity("Todos los campos son obligatorios y deben estar correctamente rellenados.").build();
		}

		// Crear un nuevo objeto Usuario con los parámetros recibidos
		Usuario u = new Usuario();
		u.setContrasena(contrasena);
		u.setUsername(username);
		u.setNombre(nombre);
		u.setRol(id_rol);
		u.setCorreo(correo);

		try {
			// Intentar registrar el nuevo usuario a través del servicio
			usuarioService.registrar(u);

			// Retornar una respuesta exitosa
			return Response.status(Response.Status.OK).entity(u).build();

		} catch (DataException | ServiceException e) {
			// Registro del error para depuración interna
			logger.error("Error al registrar el usuario: ", e);

			// Retornar una respuesta con un error interno
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Ha ocurrido un error interno al registrar el usuario.").build();
		}
	}

	@Path("/find/{id}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "Busqueda por id de usuario", description = "Recupera todos los datos de un usuario por su id", responses = {
			@ApiResponse(responseCode = "200", description = "Usuario encontrado", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Usuario.class))),
			@ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
			@ApiResponse(responseCode = "400", description = "Error al recuperar los datos") })
	public Response getById(@PathParam("id") Long id) {

		try {
			Usuario usuario = usuarioService.findById(id);
			if (usuario == null) {
				return Response.status(Status.NOT_FOUND).entity("Usuario con ID " + id + " no encontrado.").build();
			}
			return Response.ok(usuario).build();
		} catch (DataException e) {
			// Registro del error para depuración interna
			e.getMessage(); // Cambiar por un logger en producción
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity("Ha ocurrido un error interno al buscar el usuario.").build();
		}
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "Buscar usuarios por múltiples criterios", description = "Permite buscar usuarios utilizando diferentes criterios como id, username, nombre, id_rol, y correo. El resultado es una lista de usuarios que coinciden con los parámetros proporcionados.", responses = {
			@ApiResponse(responseCode = "200", description = "Usuarios encontrados", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Results.class))),
			@ApiResponse(responseCode = "404", description = "Parámetros de búsqueda inválidos"),
			@ApiResponse(responseCode = "500", description = "Error interno al buscar los usuarios") })

	public Response findBy(@QueryParam("id") Long id, @QueryParam("username") String username,
			@QueryParam("nombre") String nombre, @QueryParam("id_rol") Long idRol,
			@QueryParam("correo") String correo) {
		// Crear criterios basados en los parámetros de entrada
		UsuarioCriteria criteria = new UsuarioCriteria();
		if (id != null) {
			criteria.setId(id);
		}
		if (username != null) {
			criteria.setUsername(username);
		}
		if (nombre != null) {
			criteria.setNombre(nombre);
		}
		if (idRol != null) {
			criteria.setRol(idRol);
		}
		if (correo != null) {
			criteria.setCorreo(correo);
		}

		Results<Usuario> resultados;
		try {
			// Llamar al servicio para obtener los resultados
			resultados = usuarioService.findBy(criteria, 1, 10);
			if (resultados == null) {
				return Response.status(Response.Status.NOT_FOUND)
						.entity("No se encontraron usuarios que coincidan con los criterios de búsqueda.").build();
			}
		} catch (DataException e) {
			// Manejo de errores: devolver respuesta con el mensaje de error
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Error al obtener los usuarios: " + e.getMessage()).build();
		}
		// Devolver la lista de usuarios en formato JSON
		return Response.ok(resultados).build();
	}

	@Path("/delete/{id}")
	@DELETE
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "Eliminar un usuario por ID", description = "Este endpoint permite eliminar un usuario del sistema utilizando su ID.", responses = {
			@ApiResponse(responseCode = "200", description = "Usuario eliminado exitosamente", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = String.class))),
			@ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
			@ApiResponse(responseCode = "500", description = "Error interno en el servidor al intentar eliminar el usuario") })
	public Response deleteUsuario(@PathParam("id") Long id) {
		try {
			logger.info("Intentando eliminar usuario con ID: " + id);
			Usuario usuario = usuarioService.findById(id);
			if (usuario == null) {
				logger.warn("Usuario con ID " + id + " no encontrado.");
				return Response.status(Status.NOT_FOUND).entity("Usuario con ID " + id + " no encontrado.").build();
			}

			logger.info("Usuario encontrado, procediendo a eliminar...");
			try {
				usuarioService.delete(id);
			} catch (ServiceException e) {
				e.getMessage();
			}
			logger.info("Usuario con ID " + id + " eliminado exitosamente.");
			return Response.ok("Usuario con ID " + id + " eliminado exitosamente.").build();
		} catch (DataException e) {
			logger.error("Error al eliminar el usuario con ID " + id, e);
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity("Ha ocurrido un error interno al eliminar el usuario: " + e.getMessage()).build();
		}
	}

	@PUT
	@Path("/update/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "Actualizar un usuario por ID", description = "Permite actualizar los datos de un usuario existente utilizando su ID.", responses = {
			@ApiResponse(responseCode = "200", description = "Usuario actualizado exitosamente", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Usuario.class))),
			@ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
			@ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
			@ApiResponse(responseCode = "500", description = "Error interno al actualizar el usuario") })
	public Response updateUsuario(@PathParam("id") Long id, Usuario updatedUsuario) {
		try {
			logger.info("Intentando actualizar usuario con ID: " + id);

			// Verificar si el usuario existe
			Usuario existingUsuario = usuarioService.findById(id);
			if (existingUsuario == null) {
				logger.warn("Usuario con ID " + id + " no encontrado.");
				return Response.status(Status.NOT_FOUND).entity("Usuario con ID " + id + " no encontrado.").build();
			}

			// Actualizar los datos del usuario
			if (updatedUsuario.getContrasena() != null) {
				existingUsuario.setContrasena(updatedUsuario.getContrasena());
			}
			if (updatedUsuario.getUsername() != null) {
				existingUsuario.setUsername(updatedUsuario.getUsername());
			}
			if (updatedUsuario.getNombre() != null) {
				existingUsuario.setNombre(updatedUsuario.getNombre());
			}
			if (updatedUsuario.getRol() != null) {
				existingUsuario.setRol(updatedUsuario.getRol());
			}
			if (updatedUsuario.getCorreo() != null) {
				existingUsuario.setCorreo(updatedUsuario.getCorreo());
			}

			// Guardar los cambios
			usuarioService.update(existingUsuario);

			logger.info("Usuario con ID " + id + " actualizado exitosamente.");
			return Response.ok(existingUsuario).build();

		} catch (DataException | ServiceException e) {
			logger.error("Error al actualizar el usuario con ID " + id, e);
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity("Ha ocurrido un error interno al actualizar el usuario: " + e.getMessage()).build();
		}
	}
	
}
