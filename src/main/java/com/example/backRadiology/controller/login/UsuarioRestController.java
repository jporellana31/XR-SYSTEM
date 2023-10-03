package com.example.backRadiology.controller.login;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.provider.approval.ApprovalStore;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.backRadiology.infraestructure.repository.login.RoleRepositorio;
import com.example.backRadiology.infraestructure.repository.login.UserRepository;
import com.example.backRadiology.infraestructure.services.login.EmailSenderService;
import com.example.backRadiology.infraestructure.services.login.IUsuarioService;
import com.example.backRadiology.model.login.Role;
import com.example.backRadiology.model.login.Usuario;

import org.springframework.data.domain.Sort;
import javax.mail.MessagingException;

@CrossOrigin(origins = { "*" })
@RestController
@RequestMapping("/api")
public class UsuarioRestController {

	@Autowired
	private IUsuarioService usuarioService;

	@Autowired
	private RoleRepositorio roleRepositorio;

	@Autowired
	private BCryptPasswordEncoder passwordEncoder;

	@Autowired
	private EmailSenderService senderService;


	@Autowired
	TokenStore tokenStore;
	
	@Autowired
	ApprovalStore approvalStore;

	// private final Logger log =
	// LoggerFactory.getLogger(ClienteRestController.class);
	@Secured({ "ROLE_USER" })
	@GetMapping("/hola")
	public String hola()
	{
		return "hola";
	}

	@Autowired
    UserRepository userRepository;

	// GET PERSONAS
    @GetMapping("/personas")
    @ResponseStatus(HttpStatus.OK)
    public List<Usuario> getPersonas(){
		List<Usuario> personas = new ArrayList<Usuario>();
      personas = userRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
      return personas;
    }

	// GET PERSONAS ID
	@GetMapping("/persona/{id}")
    @ResponseStatus(HttpStatus.OK)
	public Usuario getById(@PathVariable int id) {
		Usuario usuario = userRepository.findById(Long.valueOf(id)).get();
		return usuario;
	}
	
	@GetMapping("/user/{email}")
    @ResponseStatus(HttpStatus.OK)
	public Usuario getByEmail(@PathVariable String email) {
		Usuario userr = userRepository.findByEmail(email).get();
		return userr;
	}



	@PostMapping("/usuario")
	// @ResponseStatus(HttpStatus.OK)
	public ResponseEntity<?> create(@RequestBody Usuario usuario, BindingResult result) throws Exception {
		Usuario usuarioNew = null;
		Map<String, Object> response = new HashMap<>();

		if (result.hasErrors()) {

			List<String> errors = result.getFieldErrors().stream()
					.map(err -> "El campo '" + err.getField() + "' " + err.getDefaultMessage())
					.collect(Collectors.toList());

			response.put("errors", errors);
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.BAD_REQUEST);
		}

		try {
			String passwordBcrypt = passwordEncoder.encode(usuario.getPassword());

			usuario.setEstado(false);
			usuario.setEstadoTokenRegistro(false);
			usuario.setRevocarToken(false);
			usuario.setPassword(passwordBcrypt);
			String emailEncripted = passwordEncoder.encode(usuario.getEmail());
			emailEncripted = emailEncripted.replace("/", "J");
			
			usuario.setEmailEncripted(emailEncripted);
			usuarioNew = usuarioService.save(usuario);

		} catch (DataAccessException e) {
			response.put("mensaje", "Su cédula o correo electrónico ya existen en nuestro sistema");
			response.put("error", e.getMessage().concat(": ").concat(e.getMostSpecificCause().getMessage()));
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}

		response.put("mensaje", "El cliente ha sido creado con éxito!");
		response.put("cliente", usuarioNew);
		try {
		} catch (Exception e) {
		}

		return new ResponseEntity<Map<String, Object>>(response, HttpStatus.CREATED);
	}

	// ACTUALOIZAR USUARIO
	@Secured({ "ROLE_ADMIN", "ROLE_USER", "ROLE_PUBLICATOR" })
	@PutMapping("/usuario/{email}")
	public ResponseEntity<?> update(@RequestBody Usuario usuario, BindingResult result, @PathVariable String email) {

		Usuario usuarioActual = usuarioService.findByEmail(email);

		Usuario usuarioUpdated = null;

		Map<String, Object> response = new HashMap<>();

		if (result.hasErrors()) {

			List<String> errors = result.getFieldErrors().stream()
					.map(err -> "El campo '" + err.getField() + "' " + err.getDefaultMessage())
					.collect(Collectors.toList());

			response.put("errors", errors);
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.BAD_REQUEST);
		}

		if (usuarioActual == null) {
			response.put("mensaje", "Error: no se pudo editar, el cliente ID: "
					.concat(email.toString().concat(" no existe en la base de datos!")));
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.NOT_FOUND);
		}

		try {

			usuarioActual.setEmail(usuario.getEmail());
			usuarioActual.setApellido(usuario.getApellido());
			usuarioActual.setNombre(usuario.getNombre());
			usuarioActual.setTelefono(usuario.getTelefono());
			usuarioActual.setDireccion(usuario.getDireccion());
			usuarioActual.setEstado(usuario.getEstado());
			if(usuarioActual.getEstadoTokenRegistro()==null)
			{
				usuarioActual.setEstadoTokenRegistro(false);
			}
			if(!usuarioActual.getEstado())
			{
				usuarioActual.setRevocarToken(true);
			}

			usuarioUpdated = usuarioService.save(usuarioActual);

		} catch (DataAccessException e) {
			response.put("mensaje", "Error al actualizar el cliente en la base de datos");
			response.put("error", e.getMessage().concat(": ").concat(e.getMostSpecificCause().getMessage()));
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}

		response.put("mensaje", "El cliente ha sido actualizado con éxito!");
		response.put("cliente", usuarioUpdated);

		return new ResponseEntity<Map<String, Object>>(response, HttpStatus.CREATED);
	}

	/// actualizar roles del usuario
	@Secured({ "ROLE_ADMIN" })
	@PutMapping("/usuario/roles/{email}")
	public ResponseEntity<?> updateRolesUsuario(@RequestBody List<String> rolesString, BindingResult result,
			@PathVariable String email) {

		Usuario usuarioActual = usuarioService.findByEmail(email);


		Usuario usuarioUpdated = null;


		List<Role> allRoles = roleRepositorio.findAll();
		List<Role> selectedRoles = new ArrayList<Role>();

		Map<String, Object> response = new HashMap<>();

		if (result.hasErrors()) {

			List<String> errors = result.getFieldErrors().stream()
					.map(err -> "El campo '" + err.getField() + "' " + err.getDefaultMessage())
					.collect(Collectors.toList());

			response.put("errors", errors);
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.BAD_REQUEST);
		}

		if (usuarioActual == null) {
			response.put("mensaje", "Error: no se pudo editar, el cliente ID: "
					.concat(email.toString().concat(" no existe en la base de datos!")));
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.NOT_FOUND);
		}

		try {

			for (Role role : allRoles) {

				for (String roleString : rolesString) {

					if (role.getNombre().equals(roleString)) {
						selectedRoles.add(role);
					}

				}

			}
			usuarioActual.setRoles(selectedRoles);
			usuarioActual.setRevocarToken(true);

			usuarioUpdated = usuarioService.save(usuarioActual);
			// tokenServices.revokeToken(token);

		} catch (DataAccessException e) {
			response.put("mensaje", "Error al actualizar el cliente en la base de datos");
			response.put("error", e.getMessage().concat(": ").concat(e.getMostSpecificCause().getMessage()));
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}

		response.put("mensaje", "El cliente ha sido actualizado con éxito!");
		response.put("cliente", usuarioUpdated);

		return new ResponseEntity<Map<String, Object>>(response, HttpStatus.CREATED);
	}

	/// Este metodo es para confirmar el login del usuario
	@GetMapping("/usuario/actualizar/{emailCripted}")
	public ResponseEntity<?> updateUserEstado(@PathVariable String emailCripted) {
		Usuario usuarioActual = usuarioService.findByEmailCri(emailCripted);

		Usuario usuarioUpdated = null;

		Map<String, Object> response = new HashMap<>();

		if (usuarioActual == null) {
			response.put("mensaje",
					"Error: no se pudo editar, el cliente ID: ".concat(" no existe en la base de datos!"));
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.NOT_FOUND);
		}

		if (usuarioActual.getEstadoTokenRegistro()) {
			response.put("mensaje", "El Token está caducado");
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.NOT_FOUND);
		}

		try {

			usuarioActual.setEstado(true);
			usuarioActual.setEstadoTokenRegistro(true);
			usuarioUpdated = usuarioService.save(usuarioActual);

		} catch (DataAccessException e) {
			response.put("mensaje", "Error al actualizar el cUsuario en la base de datos");
			response.put("error", e.getMessage().concat(": ").concat(e.getMostSpecificCause().getMessage()));
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}

		response.put("mensaje", "El cliente ha sido actualizado con éxito!");
		response.put("cliente", usuarioUpdated);

		return new ResponseEntity<Map<String, Object>>(response, HttpStatus.CREATED);
	}

	/// Este metodo es para cambiar passwor del usuario
	@Secured({ "ROLE_ADMIN", "ROLE_USER", "ROLE_PUBLICATOR" })
	@PutMapping("/usuario/{email}/{passwordNuevo}")
	public ResponseEntity<?> updatePassword(@RequestBody String passwordAnterior, BindingResult result,@PathVariable String email, @PathVariable String passwordNuevo) {

		Usuario usuarioActual = usuarioService.findByEmail(email);
		passwordNuevo = passwordEncoder.encode(passwordNuevo);
		// passwordAnterior= passwordAnterior;
		String passworUsuarioActual = usuarioActual.getPassword();

		Usuario usuarioUpdated = null;

		Map<String, Object> response = new HashMap<>();

		if (result.hasErrors()) {

			List<String> errors = result.getFieldErrors().stream()
					.map(err -> "El campo '" + err.getField() + "' " + err.getDefaultMessage())
					.collect(Collectors.toList());

			response.put("errors", errors);
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.BAD_REQUEST);
		}
		System.out.println(passwordAnterior);
		System.out.println(passworUsuarioActual);
		// if (!passwordEncoder.matches(passwordAnterior, passworUsuarioActual)) {
		// 	response.put("mensaje", "Password Incorrecto");
		// 	return new ResponseEntity<Map<String, Object>>(response, HttpStatus.NOT_FOUND);
		// }

		try {

			usuarioActual.setPassword(passwordNuevo);

			usuarioUpdated = usuarioService.save(usuarioActual);

		} catch (DataAccessException e) {
			response.put("mensaje", "Error al actualizar el cliente en la base de datos");
			response.put("error", e.getMessage().concat(": ").concat(e.getMostSpecificCause().getMessage()));
			return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}

		response.put("mensaje", "El cliente ha sido actualizado con éxito!");
		response.put("cliente", usuarioUpdated);

		return new ResponseEntity<Map<String, Object>>(response, HttpStatus.ACCEPTED);
	}

	@Secured({ "ROLE_ADMIN" })
	@GetMapping("/usuarios/{terminoBusqueda}")
	@ResponseStatus(HttpStatus.OK)
	public List<Usuario> filtrarProductos(@PathVariable String terminoBusqueda) {
		return usuarioService.findByNombreAndApellido(terminoBusqueda);
	}

	@Secured({ "ROLE_ADMIN" })
	@GetMapping("/roles")
	@ResponseStatus(HttpStatus.OK)
	public List<Role> findAllRoles() {
		return roleRepositorio.findAll();
	}
	
	//????????????????????????????
	@GetMapping("/usuario/password/lost/{cedula}/{email}")
	public ResponseEntity<?>  CambiarContrasena(@PathVariable String cedula, @PathVariable String email) throws MessagingException{

		Map<String, Object> response = new HashMap<>();

		Usuario usuarioUpdated = null;

		try {
			
			Usuario usuarioActual = usuarioService.findUsuarioByCedulaAndEmail(cedula, email);

			if (usuarioActual == null)
			{
				return new ResponseEntity<Map<String, Object>>(response, HttpStatus.NOT_FOUND);	
			}

			String generatedPassword = alphaNumericString(6);
			String encodePassword = new String(Base64.getEncoder().encode(generatedPassword.getBytes()));
		    
			usuarioActual.setPassword(passwordEncoder.encode(encodePassword));
			if(usuarioActual.getEstadoTokenRegistro()==null)
			{
				usuarioActual.setEstadoTokenRegistro(false);
			}
			if(!usuarioActual.getEstado())
			{
				usuarioActual.setRevocarToken(true);
			}
			usuarioUpdated = usuarioService.save(usuarioActual);

			// send email
				senderService.sendSimpleEmail(email,
				"XR CHEST, Se restablecio tu contraseña",
				"Tu nueva contraseña es: " + encodePassword);
			

		} catch (Exception e) {
			response.put("mensaje", "El cliente no existe");
		}

		response.put("mensaje", "El cliente ha sido actualizado con éxito!");
		response.put("cliente", usuarioUpdated);

		return new ResponseEntity<Map<String, Object>>(response, HttpStatus.ACCEPTED);
	}
	public static String alphaNumericString(int len) {
	    String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
	    Random rnd = new Random();

	    StringBuilder sb = new StringBuilder(len);
	    for (int i = 0; i < len; i++) {
	        sb.append(AB.charAt(rnd.nextInt(AB.length())));
	    }
	    return sb.toString();
	}
}