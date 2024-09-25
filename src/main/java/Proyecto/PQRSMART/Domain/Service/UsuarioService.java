package Proyecto.PQRSMART.Domain.Service;


import Proyecto.PQRSMART.Domain.Dto.UsuarioDto;
import Proyecto.PQRSMART.Domain.Mapper.UsuarioMapper;
import Proyecto.PQRSMART.Persistence.Entity.StateUser;
import Proyecto.PQRSMART.Persistence.Entity.User;
import Proyecto.PQRSMART.Persistence.Repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;


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
        usuarioRepository.save(UsuarioMapper.toEntity(usuarioDto));
        return usuarioDto;
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
}
