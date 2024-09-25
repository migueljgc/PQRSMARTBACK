package Proyecto.PQRSMART.Controller;

import Proyecto.PQRSMART.Domain.Dto.CategoryDTO;
import Proyecto.PQRSMART.Domain.Service.CategoryService;
import Proyecto.PQRSMART.Persistence.Entity.State;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/category")
public class CategoryController {
    @Autowired

    private CategoryService categoryService;

    @PostMapping("/save")
    public CategoryDTO save(@RequestBody CategoryDTO categoryDTO){
        return categoryService.save(categoryDTO);
    }

    @GetMapping("/get")
    public List<CategoryDTO> get(){return categoryService.getAll();}

    @PutMapping("/update")
    public ResponseEntity<?> update(@RequestBody CategoryDTO categoryDTO) {
        Optional<CategoryDTO> categoryDTOOptional = categoryService.findById(categoryDTO.getIdCategory());
        if(categoryDTOOptional.isPresent()) {
            categoryService.save(categoryDTO);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
    @PutMapping("/cancel/{id}")
    public ResponseEntity<CategoryDTO> delete(@PathVariable Long id){
        Optional<CategoryDTO> categoryDTOOptional = categoryService.findById(id);
        if(categoryDTOOptional.isPresent()) {
            CategoryDTO categoryDTO = categoryDTOOptional.get();
            categoryDTO.setState(new State(2l, "DESACTIVADO"));
            categoryService.save(categoryDTO);
            return ResponseEntity.ok(categoryDTO);
        }
        return ResponseEntity.notFound().build();
    }
}
