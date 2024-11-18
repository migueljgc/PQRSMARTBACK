package Proyecto.PQRSMART.Domain.Service;


import Proyecto.PQRSMART.Config.Exception.Exceptions;
import Proyecto.PQRSMART.Domain.Dto.UsuarioDto;
import Proyecto.PQRSMART.Domain.Mapper.UsuarioMapper;
import Proyecto.PQRSMART.Persistence.Entity.Dependence;
import Proyecto.PQRSMART.Persistence.Entity.StateUser;
import Proyecto.PQRSMART.Persistence.Entity.User;
import Proyecto.PQRSMART.Persistence.Repository.DependenceRepository;
import Proyecto.PQRSMART.Persistence.Repository.StateUserRepository;
import Proyecto.PQRSMART.Persistence.Repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    private final PasswordEncoder passwordEncoder;

    private final EmailServiceImpl emailService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private StateUserRepository stateUserRepository;

    @Autowired
    private DependenceRepository dependenceRepository;

    public List<UsuarioDto> getAll() {
        return usuarioRepository.findAll().stream().map(UsuarioMapper::toDto).collect(Collectors.toList());
    }

    public Optional<UsuarioDto> findById(Long id) {
        return usuarioRepository.findById(id).map(UsuarioMapper::toDto);
    }

    public Optional<User> findByIds(Long id) {
        return usuarioRepository.findById(id);
    }

    public UsuarioDto save(UsuarioDto usuarioDto) {
        // Busca el StateUser existente en la base de datos
        StateUser stateUser = stateUserRepository.findById(usuarioDto.getStateUser().getId())
                .orElseThrow(() -> new IllegalArgumentException("StateUser not found"));

        // Asigna el StateUser existente al usuario
        usuarioDto.setStateUser(stateUser);
        usuarioRepository.save(UsuarioMapper.toEntity(usuarioDto));
        return usuarioDto;
    }

    public User saveCorreo(User usuario) {

        usuarioRepository.save(usuario);

        return usuario;
    }

    public User saves(User usuario) {
        usuarioRepository.save(usuario);
        return usuario;
    }

    public UsuarioDto upda(UsuarioDto userDTO) {
        Optional<User> existingUserOptional = usuarioRepository.findById(userDTO.getId());
        if (existingUserOptional.isPresent()) {
            User existingUser = existingUserOptional.get();
            existingUser.setStateUser(userDTO.getStateUser());
            usuarioRepository.save(existingUser);
            return userDTO;
        } else {
            usuarioRepository.save(UsuarioMapper.toEntity(userDTO));
            return userDTO;
        }
    }


    public void verifyUser(String username) {

        User user = usuarioRepository.findByUser(username);
        if (user != null){
            user.setStateUser(new StateUser(2L, "ACTIVO"));
            usuarioRepository.save(user);
        }
    }

    public User findByEmail(String email) {
        return usuarioRepository.findByEmail(email) ;
    }



    public void resetPassword(String email, String newPassword) {
        User user = usuarioRepository.findByEmail(email);
        if (user != null){
            user.setPassword(passwordEncoder.encode(newPassword));
            usuarioRepository.save(user);
        }
        else{
            System.out.println("Email no encontrado");
        }
    }

    public boolean existsByEmail(String email) {
        // Verificar si el email ya existe
        return usuarioRepository.existsByEmail(email) ;
    }

    public UsuarioDto update(UsuarioDto usuarioDto) {
        System.out.println(usuarioDto);
// Busca el usuario existente en la base de datos para validar
        User usuario = usuarioRepository.findById(usuarioDto.getId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // Busca el StateUser existente en la base de datos
        StateUser stateUser = stateUserRepository.findById(usuarioDto.getStateUser().getId())
                .orElseThrow(() -> new IllegalArgumentException("StateUser not found"));

        // Asigna el StateUser existente al usuario
        usuarioDto.setStateUser(stateUser);
        // Asigna los nuevos valores al UsuarioDto
        usuario.setName(usuarioDto.getName());
        usuario.setLastName(usuarioDto.getLastName());
        usuario.setEmail(usuarioDto.getEmail());
        usuario.setRole(usuarioDto.getRole());
        Optional<Dependence> dependence = dependenceRepository.findById(usuarioDto.getDependence().getIdDependence());
        if(dependence.isPresent()){
            usuario.setDependence(dependence.get());
            System.out.println(usuario);
        };

        usuarioRepository.save(usuario);
        // Devuelve el DTO actualizado (opcional, dependiendo de tus necesidades)
        return UsuarioMapper.toDto(usuario);
    }
}
