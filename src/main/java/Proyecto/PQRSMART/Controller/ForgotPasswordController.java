package Proyecto.PQRSMART.Controller;

import Proyecto.PQRSMART.Domain.Service.EmailServiceImpl;
import Proyecto.PQRSMART.Domain.Service.JwtService;
import Proyecto.PQRSMART.Domain.Service.UsuarioService;
import Proyecto.PQRSMART.Persistence.Entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/forgot-password")
public class ForgotPasswordController {

    @Autowired
    private UsuarioService userService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private EmailServiceImpl emailService;


    @PostMapping("/email")
    public ResponseEntity<String> requestPasswordReset(@RequestBody String email) {
        try {


        User user = userService.findByEmail(email);
        if (user == null) {
            return ResponseEntity.badRequest().body("No se encontró un usuario con ese correo electrónico.");
        }

        // Generar token JWT para restablecer la contraseña
        String token = jwtService.genereteTokenEmail(user.getEmail());

        // Construir el enlace para restablecer la contraseña
        String resetLink = "https://pqrsmartfront.onrender.com/reset-password/" + token;

        // Enviar correo electrónico con el enlace de restablecimiento
        String message = String.format("<h1>Para restablecer tu contraseña, haz clic en este enlace: <h1/>" + "<a href=\"%s\">Restablecer Contraseña</a>",resetLink );
        emailService.sendEmails(
                new String[]{email},
                "Recuperación de contraseña",
                message);
        }catch (Exception e){
            System.out.println(e);
        }
        return ResponseEntity.ok("Se ha enviado un correo electrónico con las instrucciones para restablecer la contraseña.");
    }

    @PostMapping("/reset/{token}")
    public ResponseEntity<String> resetPassword(@PathVariable String token, @RequestBody Map<String, String> payload) {
        String newPassword=payload.get("newPassword");
        if (jwtService.validateTokenForPasswordReset(token)) {
            String username = jwtService.getUserName(token);
            userService.resetPassword(username, newPassword);

            return ResponseEntity.ok("Contraseña restablecida exitosamente.");
        } else {
            return ResponseEntity.badRequest().body("El enlace de restablecimiento de contraseña es inválido o ha expirado.");
        }
    }
}