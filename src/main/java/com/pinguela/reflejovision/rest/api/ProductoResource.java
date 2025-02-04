package com.pinguela.reflejovision.rest.api;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
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
import com.luis.reflejovision.model.MateriaPrimaDTO;
import com.luis.reflejovision.model.Producto;
import com.luis.reflejovision.model.ProductoCriteria;
import com.luis.reflejovision.model.Results;
import com.luis.reflejovision.service.ProductoService;
import com.luis.reflejovision.service.StockException;
import com.luis.reflejovision.service.impl.ProductoServiceImpl;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@Path("/producto")
public class ProductoResource {

	private ProductoService productoService = null;
	private static Logger logger = LogManager.getLogger(ProductoResource.class);

	public ProductoResource() {
		productoService = new ProductoServiceImpl();
	}

	@GET
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "Obtener una producto por ID", description = "Este endpoint permite obtener un producto del sistema por su ID.", responses = {
			@ApiResponse(responseCode = "200", description = "producto encontrado", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = MateriaPrimaDTO.class))),
			@ApiResponse(responseCode = "404", description = "producto no encontrado"),
			@ApiResponse(responseCode = "500", description = "Error interno en el servidor al intentar obtener la producto") })
	public Response findById(
			@Parameter(description = "ID del producto a buscar", required = true) @PathParam("id") Long id) {

		try {
			logger.info("Buscando producto con ID: " + id);

			Producto p = productoService.findById(id);

			if (p == null) {
				logger.warn("producto con ID " + id + " no encontrada.");
				return Response.status(Status.NOT_FOUND).entity("producto con ID " + id + " no encontrada.").build();
			}

			logger.info("producto con ID " + id + " encontrado.");
			return Response.status(Status.OK).entity(p).build();
		} catch (PinguelaException pe) {
			logger.error("Error al buscar la producto con ID: " + id, pe);
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity("Ha ocurrido un error interno al buscar el producto: " + pe.getMessage()).build();
		} catch (Exception e) {
			logger.error("Error inesperado al buscar la producto con ID: " + id, e);
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity("Ha ocurrido un error inesperado al buscar el producto: " + e.getMessage()).build();
		}
	}

	@GET
	@Path("/search")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "Buscar productos por criterios", description = "Este endpoint permite buscar productos aplicando filtros opcionales como ID, nombre, rango de precios, cantidad de unidades, y localización.", responses = {
			@ApiResponse(responseCode = "200", description = "Productos encontrados", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Results.class))),
			@ApiResponse(responseCode = "400", description = "Criterios de búsqueda no proporcionados o inválidos"),
			@ApiResponse(responseCode = "404", description = "No se encontraron productos con los criterios proporcionados"),
			@ApiResponse(responseCode = "500", description = "Error interno en el servidor al procesar la búsqueda") })
	public Response findByCriteria(@QueryParam("id") Long id, @QueryParam("unidadesDesde") Integer unidadesDesde,
			@QueryParam("unidadesHasta") Integer unidadesHasta, @QueryParam("nombre") String nombre,
			@QueryParam("precioDesde") Double precioDesde, @QueryParam("precioHasta") Double precioHasta,
			@QueryParam("locale") String locale) {

		try {
			logger.info("Criterios recibidos: id=" + id + ", unidadesDesde=" + unidadesDesde + ", unidadesHasta="
					+ unidadesHasta + ", nombre=" + nombre + ", precioDesde=" + precioDesde + ", precioHasta="
					+ precioHasta + ", locale=" + locale);

			ProductoCriteria criteria = new ProductoCriteria();
			criteria.setId(id);
			criteria.setUnidadesDesde(unidadesDesde);
			criteria.setUnidadesHasta(unidadesHasta);
			criteria.setNombre(nombre);
			criteria.setPrecioDesde(precioDesde);
			criteria.setPrecioHasta(precioHasta);
			criteria.setLocale(locale);

			Results<Producto> resultados = productoService.findBy(criteria, 1, 10);

			if (resultados == null) {
				logger.warn("No se encontraron resultados con los criterios: " + criteria);
				return Response.status(Response.Status.NOT_FOUND)
						.entity("No se encontraron productos con los criterios proporcionados.").build();
			}

			return Response.ok(resultados).build();
		} catch (Exception e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Error al buscar productos: " + e.getMessage()).build();
		}
	}

	@POST
	@Path("/create")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "Crear un nuevo producto", description = "Este endpoint permite crear un nuevo producto en el sistema.", responses = {
			@ApiResponse(responseCode = "201", description = "Producto creado exitosamente"),
			@ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
			@ApiResponse(responseCode = "500", description = "Error interno en el servidor al intentar crear el producto") })
	public Response createProducto(
			@RequestBody(description = "Datos del producto a crear", required = true) Producto producto) {

		try {
			logger.info("Intentando crear un nuevo producto: " + producto);

			// Validar los datos del producto
			if (producto.getNombre() == null || producto.getNombre().isEmpty()) {
				return Response.status(Status.BAD_REQUEST).entity("El nombre del producto es obligatorio.").build();
			}
			if (producto.getPrecio() == null || producto.getPrecio() <= 0) {
				return Response.status(Status.BAD_REQUEST).entity("El precio del producto debe ser mayor que 0.")
						.build();
			}

			producto.setUnidades(0);

			// Crear el producto utilizando el servicio
			Long id = productoService.create(producto);

			// Devolver la respuesta con el ID del producto creado
			logger.info("Producto creado: " + producto);
			return Response.status(Status.CREATED).entity("Producto creado exitosamente con ID: " + id).build();

		} catch (PinguelaException pe) {
			logger.error("Error al crear el producto: " + producto, pe);
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity("Ha ocurrido un error interno al crear el producto: " + pe.getMessage()).build();
		} catch (Exception e) {
			logger.error("Error inesperado al crear el producto: " + producto, e);
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity("Ha ocurrido un error inesperado al crear el producto: " + e.getMessage()).build();
		}
	}

	@PUT
	@Path("/update-stock/{idProducto}/{variacionStock}/{locale}")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "Actualizar stock de un producto", description = "Este endpoint permite actualizar la cantidad en stock de un producto.", responses = {
			@ApiResponse(responseCode = "200", description = "Stock actualizado exitosamente"),
			@ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
			@ApiResponse(responseCode = "500", description = "Error interno en el servidor al intentar actualizar el stock") })
	public Response updateStock(@PathParam("idProducto") Long idProducto,
			@PathParam("variacionStock") Integer variacionStock, @PathParam("locale") String locale) {

		try {
			logger.info("Intentando actualizar el stock del producto ID: " + idProducto);

			// Validar los datos de entrada
			if (idProducto == null) {
				return Response.status(Status.BAD_REQUEST).entity("El ID del producto es obligatorio.").build();
			}
			if (variacionStock == null || variacionStock <= 0) {
				return Response.status(Status.BAD_REQUEST).entity("La variación de stock debe ser mayor que 0.")
						.build();
			}

			// Llamar al servicio con actualizacionAutomaticaMateriasPrimas = true
			productoService.updateStock(idProducto, variacionStock, true, locale);

			// Devolver la respuesta
			return Response.ok().entity("Stock actualizado exitosamente para el producto ID: " + idProducto).build();

		} catch (StockException se) {
			logger.error("Error de stock al actualizar el producto: " + idProducto, se);
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity("Error al actualizar el stock: " + se.getMessage()).build();
		} catch (Exception e) {
			logger.error("Error inesperado al actualizar el stock del producto: " + idProducto, e);
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity("Ha ocurrido un error inesperado al actualizar el stock: " + e.getMessage()).build();
		}
	}

	@DELETE
	@Path("/delete/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "Eliminar un producto", description = "Este endpoint permite eliminar un producto del sistema por su ID.", responses = {
			@ApiResponse(responseCode = "200", description = "Producto eliminada exitosamente"),
			@ApiResponse(responseCode = "404", description = "Producto no encontrada"),
			@ApiResponse(responseCode = "500", description = "Error interno en el servidor al intentar eliminar el Producto") })
	public Response deleteMateriaPrima(
			@Parameter(description = "ID del Producto a eliminar", required = true) @PathParam("id") Long id) {

		try {
			logger.info("Intentando eliminar la producto con ID: " + id);

			// Intentar eliminar la producto
			boolean eliminado = productoService.delete(id);

			if (eliminado) {
				logger.info("Producto con ID " + id + " eliminada exitosamente.");
				return Response.status(Status.OK).entity("producto con ID " + id + " eliminada exitosamente.").build();
			} else {
				logger.warn("Producto con ID " + id + " no encontrada.");
				return Response.status(Status.NOT_FOUND).entity("producto con ID " + id + " no encontrada.").build();
			}
		} catch (PinguelaException pe) {
			logger.error("Error al eliminar Producto con ID: " + id, pe);
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity("Ha ocurrido un error interno al eliminar Producto: " + pe.getMessage()).build();
		} catch (Exception e) {
			logger.error("Error inesperado al eliminar Producto con ID: " + id, e);
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity("Ha ocurrido un error inesperado al eliminar Producto: " + e.getMessage()).build();
		}
	}

}
