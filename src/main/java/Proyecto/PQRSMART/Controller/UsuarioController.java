package Proyecto.PQRSMART.Controller;

import Proyecto.PQRSMART.Config.Exception.Exceptions;
import Proyecto.PQRSMART.Domain.Dto.UsuarioDto;
import Proyecto.PQRSMART.Domain.Service.EmailServiceImpl;
import Proyecto.PQRSMART.Domain.Service.JwtService;
import Proyecto.PQRSMART.Domain.Service.UsuarioService;
import Proyecto.PQRSMART.Persistence.Entity.RequestState;
import Proyecto.PQRSMART.Persistence.Entity.StateUser;
import Proyecto.PQRSMART.Persistence.Entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/Usuario")
public class UsuarioController {
    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private EmailServiceImpl emailService;

    @Autowired
    private JwtService jwtService;

    @PostMapping("/save")
    public UsuarioDto save(@RequestBody UsuarioDto usuarioDto){
        return usuarioService.save(usuarioDto);
    }

    @PostMapping("/saves")
    public User saves(@RequestBody User usuario){
        return usuarioService.saves(usuario);
    }

    @GetMapping("/get")
    public List<UsuarioDto> get(){return usuarioService.getAll();}

    @PutMapping("/Update")
    public ResponseEntity<?> update(@RequestBody UsuarioDto usuarioDto) {
        Optional<UsuarioDto> personTypeDTOOptional = usuarioService.findById(usuarioDto.getId());
        if(personTypeDTOOptional.isPresent()) {
            usuarioService.save(usuarioDto);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody UsuarioDto usuarioDto) {
        Optional<UsuarioDto> userDTOOptional = usuarioService.findById(id);
        if (userDTOOptional.isPresent()) {
            UsuarioDto existingUsuario = userDTOOptional.get();
            existingUsuario.setStateUser(usuarioDto.getStateUser());
            // Actualizar otros campos si es necesario
            UsuarioDto updatedRequestDTO = usuarioService.save(existingUsuario); // Guardar los cambios en la solicitud existente
            return ResponseEntity.ok(updatedRequestDTO);
        }
        return ResponseEntity.notFound().build();
    }

    @PatchMapping("/cancel/{id}")
    public ResponseEntity<User> desactivarUsuario(@PathVariable Long id) {
        Optional<User> userOptional = usuarioService.findByIds(id);
        if (userOptional.isPresent()) {
            User usuario = userOptional.get();
            // Asignar el estado "CANCELADA" de la entidad RequestState
            usuario.setStateUser(new StateUser(3L, "DESACTIVADO"));
            usuarioService.saves(usuario);
            return ResponseEntity.ok(usuario);
        }
        return ResponseEntity.notFound().build();
    }

    @PatchMapping("/activate/{id}")
    public ResponseEntity<User> activarUsuario(@PathVariable Long id) {
        Optional<User> userOptional = usuarioService.findByIds(id);
        if (userOptional.isPresent()) {
            System.out.println(userOptional);
            User usuario = userOptional.get();
            // Asignar el estado "ACTIVO" de la entidad RequestState
            usuario.setStateUser(new StateUser(2L, "ACTIVO"));
            usuarioService.saves(usuario);
            return ResponseEntity.ok(usuario);
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/Update-correo")
    public ResponseEntity<?> updateCorreo(@RequestBody Map<String, String> email) {
        try {
            String Email = email.get("email");
            System.out.println(email);
            Long ID = Long.valueOf(email.get("id"));
            System.out.println(Email + " y " + ID);
            if (usuarioService.existsByEmail(Email)) {
                throw new Exceptions.EmailAlreadyExistsException("El correo electrónico ya está en uso.");
            }
            Optional<User> user = usuarioService.findByIds(ID);
            if (user.isPresent()) {
                User usuario = user.get();
                String jwtToken = jwtService.genereteTokenCambioEmail(usuario.getId(), Email);
                System.out.println("token " + jwtToken);
                // Enviar correo electrónico de activación
                String activationLink1 = "http://localhost:5173/activate-email/" + jwtToken;
                String mensajeHtml = String.format(
                        "<h1>Hola %s %s</h1>" +
                                "<p>Gracias por actualizar su correo en nuestra plataforma. Para completar la verificación, por favor haz clic en el siguiente enlace:" +
                                "<br /><br />" +
                                "<a href=\"%s\">Verificar Identidad</a>" +
                                "<br /><br />" +
                                "Este enlace te llevará a una página donde podrás confirmar tu identidad. Una vez completado este paso, tu verificación estará finalizada y podrás seguir accediendo a todos los beneficios de nuestra plataforma de manera segura." +
                                "<br /><br />" +
                                "Si tienes alguna pregunta o necesitas asistencia durante este proceso, no dudes en contactarnos respondiendo a este correo." +
                                "<br /><br />" +
                                "Gracias de nuevo por tu colaboración." +
                                "<br /><br />" +
                                "<br /><br />" +
                                "<br /><br />" +
                                "PQRSmart<br /><br />",
                        usuario.getName(), usuario.getLastName(), activationLink1
                );

                emailService.sendEmails(
                        new String[]{usuario.getEmail()},
                        "Actualiza tu correo",
                        mensajeHtml
                );
                return ResponseEntity.ok().build();
            }


            return ResponseEntity.notFound().build();
        }
        catch (Exceptions.EmailAlreadyExistsException e) {
            // Manejar email duplicado
            return ResponseEntity.status(HttpStatus.CONFLICT).body("El correo electrónico ya está en uso.");
        }
    }

    @PutMapping("/activate-email")
    public ResponseEntity<?> activateEmail(@RequestParam("token") String token) {
        try {
            // Extraer los valores desde el token
            Map<String, Object> extractedValues = jwtService.extractClaimsFromToken(token);
            Long userId = (Long) extractedValues.get("userId");
            String newEmail = (String) extractedValues.get("newEmail");

            // Buscar el usuario por su ID
            Optional<User> optionalUser = usuarioService.findByIds(userId);
            if (optionalUser.isPresent()) {
                User user = optionalUser.get();

                // Actualizar el correo y el estado
                user.setEmail(newEmail);

                // Guardar cambios en la base de datos
                usuarioService.saves(user);

                return ResponseEntity.ok("Email actualizado y usuario activado.");
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuario no encontrado.");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Token inválido o expirado.");
        }
    }
}
