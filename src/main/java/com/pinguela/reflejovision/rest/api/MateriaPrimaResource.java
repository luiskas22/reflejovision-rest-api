package com.pinguela.reflejovision.rest.api;

import java.util.ArrayList;
import java.util.List;

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

import com.luis.reflejovision.PinguelaException;
import com.luis.reflejovision.model.MateriaPrimaCriteria;
import com.luis.reflejovision.model.MateriaPrimaDTO;
import com.luis.reflejovision.model.MateriaPrimaIdioma;
import com.luis.reflejovision.model.Results;
import com.luis.reflejovision.service.MateriaPrimaService;
import com.luis.reflejovision.service.impl.MateriaPrimaServiceImpl;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@Path("/materiaprima")
@Singleton
public class MateriaPrimaResource {

	private MateriaPrimaService materiaPrimaService = null;
	private static Logger logger = LogManager.getLogger(MateriaPrimaResource.class);

	public MateriaPrimaResource() {
		materiaPrimaService = new MateriaPrimaServiceImpl();
	}

	@GET
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "Obtener una materia prima por ID", description = "Este endpoint permite obtener una materia prima del sistema por su ID.", responses = {
			@ApiResponse(responseCode = "200", description = "Materia prima encontrada", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = MateriaPrimaDTO.class))),
			@ApiResponse(responseCode = "404", description = "Materia prima no encontrada"),
			@ApiResponse(responseCode = "500", description = "Error interno en el servidor al intentar obtener la materia prima") })
	public Response findById(
			@Parameter(description = "ID de la materia prima a buscar", required = true) @PathParam("id") Long id) {

		try {
			logger.info("Buscando materia prima con ID: " + id);

			// Llamar al servicio para obtener la materia prima por ID
			MateriaPrimaDTO materiaPrima = materiaPrimaService.findbyId(id, "es");

			if (materiaPrima == null) {
				logger.warn("Materia prima con ID " + id + " no encontrada.");
				return Response.status(Status.NOT_FOUND).entity("Materia prima con ID " + id + " no encontrada.")
						.build();
			}

			logger.info("Materia prima con ID " + id + " encontrada.");
			return Response.status(Status.OK).entity(materiaPrima).build();
		} catch (PinguelaException pe) {
			logger.error("Error al buscar la materia prima con ID: " + id, pe);
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity("Ha ocurrido un error interno al buscar la materia prima: " + pe.getMessage()).build();
		} catch (Exception e) {
			logger.error("Error inesperado al buscar la materia prima con ID: " + id, e);
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity("Ha ocurrido un error inesperado al buscar la materia prima: " + e.getMessage()).build();
		}
	}

	@GET
	@Path("/search")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "Buscar materias primas por criterios", description = "Este endpoint permite buscar materias primas aplicando filtros opcionales como ID, nombre, rango de precios, cantidad de unidades, y localización.", responses = {
	        @ApiResponse(responseCode = "200", description = "Materias primas encontradas", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Results.class))),
	        @ApiResponse(responseCode = "400", description = "Criterios de búsqueda no proporcionados o inválidos"),
	        @ApiResponse(responseCode = "404", description = "No se encontraron materias primas con los criterios proporcionados"),
	        @ApiResponse(responseCode = "500", description = "Error interno en el servidor al procesar la búsqueda") })
	public Response findByCriteria(
	        @QueryParam("id") Long id,
	        @QueryParam("unidadesDesde") Integer unidadesDesde,
	        @QueryParam("unidadesHasta") Integer unidadesHasta,
	        @QueryParam("nombre") String nombre,
	        @QueryParam("precioDesde") Double precioDesde,
	        @QueryParam("precioHasta") Double precioHasta,
	        @QueryParam("locale") String locale) {

	    try {
	        logger.info("Criterios recibidos: id=" + id + ", unidadesDesde=" + unidadesDesde + ", unidadesHasta=" + unidadesHasta +
	                ", nombre=" + nombre + ", precioDesde=" + precioDesde + ", precioHasta=" + precioHasta + ", locale=" + locale);

	        MateriaPrimaCriteria criteria = new MateriaPrimaCriteria();
	        criteria.setId(id);
	        criteria.setUnidadesDesde(unidadesDesde);
	        criteria.setUnidadesHasta(unidadesHasta);
	        criteria.setNombre(nombre);
	        criteria.setPrecioDesde(precioDesde);
	        criteria.setPrecioHasta(precioHasta);
	        criteria.setLocale(locale);

	        Results<MateriaPrimaDTO> resultados = materiaPrimaService.findBy(criteria, 1, 10);

	        if (resultados == null) {
	            logger.warn("No se encontraron resultados con los criterios: " + criteria);
	            return Response.status(Response.Status.NOT_FOUND)
	                    .entity("No se encontraron materias primas con los criterios proporcionados.").build();
	        }

	        return Response.ok(resultados).build();
	    } catch (Exception e) {
	        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
	                .entity("Error al buscar materias primas: " + e.getMessage()).build();
	    }
	}


	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)

	@Operation(summary = "Crear una nueva materia prima", description = "Este endpoint permite crear una nueva materia prima en el sistema, incluyendo traducciones en diferentes idiomas.", responses = {
			@ApiResponse(responseCode = "200", description = "Materia prima creada exitosamente", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = MateriaPrimaDTO.class))),
			@ApiResponse(responseCode = "400", description = "Datos de entrada inválidos o incompletos"),
			@ApiResponse(responseCode = "500", description = "Error interno en el servidor al intentar crear la materia prima") })
	public Response createMateriaPrima(
			@Parameter(description = "Nombre en español", required = true) @FormParam("nombre_es") String nombreEs,
			@Parameter(description = "Nombre en inglés", required = true) @FormParam("nombre_en") String nombreEn,
			@Parameter(description = "Nombre en gallego", required = true) @FormParam("nombre_gl") String nombreGl,
			@Parameter(description = "Precio de la materia prima", required = true) @FormParam("precio") String precioStr,
			@Parameter(description = "Unidades de la materia prima", required = true) @FormParam("unidades") String unidadesStr,
			@Parameter(description = "ID de la unidad de medida", required = true) @FormParam("unidadMedida") String unidadMedidaStr) {

		try {
			logger.info("Intentando crear una nueva materia prima.");

			// Validar los datos de entrada
			if (nombreEs == null || nombreEn == null || nombreGl == null || precioStr == null || unidadesStr == null
					|| unidadMedidaStr == null) {
				logger.warn("Datos de entrada inválidos o incompletos.");
				return Response.status(Status.BAD_REQUEST).entity("Datos de entrada inválidos o incompletos.").build();
			}

			// Validar campos numéricos
			double precio;
			int unidades;
			long idUnidadMedida;
			try {
				precio = Double.parseDouble(precioStr);
				unidades = Integer.parseInt(unidadesStr);
				idUnidadMedida = Long.parseLong(unidadMedidaStr);
			} catch (NumberFormatException e) {
				logger.warn("Formato numérico inválido en los datos de entrada.");
				return Response.status(Status.BAD_REQUEST).entity("Formato numérico inválido en los datos de entrada.")
						.build();
			}

			// Crear el DTO de la materia prima
			MateriaPrimaDTO mp = new MateriaPrimaDTO();
			mp.setNombre(nombreEs); // Usamos el nombre en español como nombre principal
			mp.setPrecio(precio);
			mp.setUnidades(unidades);
			mp.setIdUnidadMedida(idUnidadMedida);

			// Crear las traducciones
			List<MateriaPrimaIdioma> traducciones = new ArrayList<>();

			MateriaPrimaIdioma traduccionEs = new MateriaPrimaIdioma();
			traduccionEs.setLocale("es");
			traduccionEs.setNombre(nombreEs);
			traduccionEs.setIdMateriaPrima(mp.getId());

			MateriaPrimaIdioma traduccionEn = new MateriaPrimaIdioma();
			traduccionEn.setLocale("en");
			traduccionEn.setNombre(nombreEn);
			traduccionEn.setIdMateriaPrima(mp.getId());

			MateriaPrimaIdioma traduccionGl = new MateriaPrimaIdioma();
			traduccionGl.setLocale("gl_ES");
			traduccionGl.setNombre(nombreGl);
			traduccionGl.setIdMateriaPrima(mp.getId());

			traducciones.add(traduccionEs);
			traducciones.add(traduccionEn);
			traducciones.add(traduccionGl);

			mp.setTraducciones(traducciones);

			// Crear la materia prima en el servicio
			Long id = materiaPrimaService.create(mp);
			logger.info("Materia prima creada exitosamente con ID: " + id);

			// Retornar la respuesta con el DTO creado
			return Response.status(Status.OK).entity(mp).build();
		} catch (PinguelaException pe) {
			logger.error("Error al crear la materia prima", pe);
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity("Ha ocurrido un error interno al crear la materia prima: " + pe.getMessage()).build();
		} catch (Exception e) {
			logger.error("Error inesperado al crear la materia prima", e);
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity("Ha ocurrido un error inesperado al crear la materia prima: " + e.getMessage()).build();
		}
	}

	@PUT
	@Path("/update/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "Actualizar una materia prima", description = "Este endpoint permite actualizar los detalles de una materia prima existente en el sistema.", responses = {
			@ApiResponse(responseCode = "200", description = "Materia prima actualizada exitosamente", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = MateriaPrimaDTO.class))),
			@ApiResponse(responseCode = "400", description = "Datos de entrada inválidos o incompletos"),
			@ApiResponse(responseCode = "404", description = "Materia prima no encontrada"),
			@ApiResponse(responseCode = "500", description = "Error interno en el servidor al intentar actualizar la materia prima") })
	public Response updateMateriaPrima(
			@Parameter(description = "ID de la materia prima a actualizar", required = true) @PathParam("id") Long id,
			@Parameter(description = "Objeto MateriaPrimaDTO con los nuevos datos", required = true) MateriaPrimaDTO materiaPrima) {

		try {
			logger.info("Intentando actualizar la materia prima con ID: " + id);

			// Validar los datos de entrada
			if (materiaPrima == null || materiaPrima.getNombre() == null || materiaPrima.getPrecio() == null
					|| materiaPrima.getUnidades() == null || materiaPrima.getIdUnidadMedida() == null) {
				logger.warn("Datos de entrada inválidos o incompletos.");
				return Response.status(Status.BAD_REQUEST).entity("Datos de entrada inválidos o incompletos.").build();
			}

			// Buscar la materia prima existente
			MateriaPrimaDTO mp = materiaPrimaService.findbyId(id, "es");
			if (mp == null) {
				logger.warn("Materia prima con ID " + id + " no encontrada.");
				return Response.status(Status.NOT_FOUND).entity("Materia prima con ID " + id + " no encontrada.")
						.build();
			}

			// Actualizar los valores
			mp.setNombre(materiaPrima.getNombre());
			mp.setPrecio(materiaPrima.getPrecio());
			mp.setUnidades(materiaPrima.getUnidades());
			mp.setIdUnidadMedida(materiaPrima.getIdUnidadMedida());

			// Actualizar las traducciones
			if (materiaPrima.getTraducciones() != null) {
				for (MateriaPrimaIdioma traduccion : materiaPrima.getTraducciones()) {
					traduccion.setIdMateriaPrima(mp.getId()); // Asignar el ID de la materia prima
				}
				mp.setTraducciones(materiaPrima.getTraducciones());
			}

			// Guardar la actualización
			materiaPrimaService.update(mp);
			logger.info("Materia prima con ID " + id + " actualizada exitosamente.");

			return Response.status(Status.OK).entity(mp).build();
		} catch (PinguelaException pe) {
			logger.error("Error al actualizar la materia prima con ID: " + id, pe);
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity("Error al actualizar la materia prima: " + pe.getMessage()).build();
		} catch (Exception e) {
			logger.error("Error inesperado al actualizar la materia prima con ID: " + id, e);
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity("Error inesperado al actualizar la materia prima: " + e.getMessage()).build();
		}
	}

	@DELETE
	@Path("delete/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "Eliminar una materia prima", description = "Este endpoint permite eliminar una materia prima del sistema por su ID.", responses = {
			@ApiResponse(responseCode = "200", description = "Materia prima eliminada exitosamente"),
			@ApiResponse(responseCode = "404", description = "Materia prima no encontrada"),
			@ApiResponse(responseCode = "500", description = "Error interno en el servidor al intentar eliminar la materia prima") })
	public Response deleteMateriaPrima(
			@Parameter(description = "ID de la materia prima a eliminar", required = true) @PathParam("id") Long id) {

		try {
			logger.info("Intentando eliminar la materia prima con ID: " + id);

			// Intentar eliminar la materia prima directamente
			boolean eliminado = materiaPrimaService.delete(id);

			if (eliminado) {
				logger.info("Materia prima con ID " + id + " eliminada exitosamente.");
				return Response.status(Status.OK).entity("Materia prima con ID " + id + " eliminada exitosamente.")
						.build();
			} else {
				logger.warn("Materia prima con ID " + id + " no encontrada.");
				return Response.status(Status.NOT_FOUND).entity("Materia prima con ID " + id + " no encontrada.")
						.build();
			}
		} catch (PinguelaException pe) {
			logger.error("Error al eliminar la materia prima con ID: " + id, pe);
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity("Ha ocurrido un error interno al eliminar la materia prima: " + pe.getMessage()).build();
		} catch (Exception e) {
			logger.error("Error inesperado al eliminar la materia prima con ID: " + id, e);
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity("Ha ocurrido un error inesperado al eliminar la materia prima: " + e.getMessage()).build();
		}
	}
}