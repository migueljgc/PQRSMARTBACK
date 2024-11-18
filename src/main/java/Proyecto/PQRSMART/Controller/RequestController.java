package Proyecto.PQRSMART.Controller;


import Proyecto.PQRSMART.Domain.Dto.RequestDTO;
import Proyecto.PQRSMART.Domain.Service.EmailServiceImpl;
import Proyecto.PQRSMART.Domain.Service.RequestServices;
import Proyecto.PQRSMART.Domain.Service.RequestStateService;
import Proyecto.PQRSMART.Persistence.Entity.*;
import Proyecto.PQRSMART.Persistence.Repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/request")
public class RequestController {

    @Autowired
    private RequestServices requestServices;

    @Autowired
    private UsuarioRepository userRepository;
    @Autowired
    private DependenceRepository dependenceRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private RequestStateService requestStateService;
    @Autowired
    private RequestStateRepository requestStateRepository;
    @Autowired
    private RequestTypeRepository requestTypeRepository;

    private final EmailServiceImpl emailService;

    //private final Path fileStorageLocation = Paths.get("files").toAbsolutePath().normalize();
    // Ruta para guardar archivos
    @Value("${file.upload-dir:/var/data/uploads}")
    private String uploadDir;

    @PostMapping("/save")
    public ResponseEntity<String> guardarSolicitud(@RequestPart("request") RequestDTO request, @RequestPart(value = "archivo", required = false) MultipartFile archivo) {


        // Obtenemos el usuario autenticado
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        // Buscamos el usuario en la base de datos
        User user = userRepository.findByUser(userDetails.getUsername());


        if (user == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Usuario no encontrado");
        }
        String archivoGuardado = null;
        if (archivo != null && !archivo.isEmpty()) {
            try {

                //Si la Carpeta no Existe se crea
                //Files.createDirectories(fileStorageLocation);
                Files.createDirectories(Paths.get(uploadDir));

                // Guardar el archivo
                String fileName = archivo.getOriginalFilename();
                System.out.println(fileName);

                // Generar un nombre único para el archivo (ejemplo con timestamp)
                String uniqueFileName = System.currentTimeMillis() + "_" + fileName;
                System.out.println(uniqueFileName);

                Path targetLocation = Paths.get(uploadDir).resolve(uniqueFileName);
                System.out.println(targetLocation);

                Files.copy(archivo.getInputStream(), targetLocation);

                // Establecer la URL del archivo
                request.setArchivo(targetLocation.toString());

                archivoGuardado = targetLocation.toString();  // Guardamos la ruta del archivo para el adjunto

            } catch (IOException e) {
                System.out.println(e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al procesar el archivo " + e.getMessage());
            }
        }
        // Creamos la solicitud
        request.setUser(user);

        // Guardar solicitud usando el servicio
        RequestDTO savedRequest;
        Dependence dependence;
        Category category;
        RequestState requestState;
        RequestType requestType;

        try {
            savedRequest = requestServices.saves(request);
            dependence= dependenceRepository.findById(savedRequest.getDependence().getIdDependence()).orElseThrow(() -> new IllegalArgumentException("Id no encontrado"));
            category= categoryRepository.findById(savedRequest.getCategory().getIdCategory()).orElseThrow(() -> new IllegalArgumentException("Id no encontrado"));
            requestState= requestStateRepository.findById(savedRequest.getRequestState().getIdRequestState()).orElseThrow(() -> new IllegalArgumentException("Id no encontrado"));
            requestType= requestTypeRepository.findById(savedRequest.getRequestType().getIdRequestType()).orElseThrow(() -> new IllegalArgumentException("Id no encontrado"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al guardar la solicitud");
        }

        try{

                // Generar PDF con iText

                ByteArrayOutputStream pdfOutputStream = new ByteArrayOutputStream();

                // 1. Crear el documento PDF
                PdfWriter writer = new PdfWriter(pdfOutputStream);
                PdfDocument pdfDoc = new PdfDocument(writer);
                Document document = new Document(pdfDoc);

                // 2. Añadir contenido al PDF
                document.add(new Paragraph("Fecha: " + request.getDate()));
                document.add(new Paragraph(" "));
                document.add(new Paragraph(" "));
                document.add(new Paragraph("Usuario: " + user.getName()));
                document.add(new Paragraph(" "));
                document.add(new Paragraph("Tipo de Solicitud: " + requestType.getNameRequestType()));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Dependencia: " + dependence.getNameDependence()));
                document.add(new Paragraph(" "));
                document.add(new Paragraph("Categoría: " + category.getNameCategory()));
                document.add(new Paragraph(" "));
                document.add(new Paragraph("Detalle de la Solicitud: " ));
                document.add(new Paragraph( request.getDescription()));
                document.add(new Paragraph(" "));
                document.add(new Paragraph("Estado Actual de la Solicitud: " + requestState.getNameRequestState()));

                document.close();

                // Enviar el PDF por correo
                emailService.sendEmailWithPdf(user.getEmail(), "Detalle de Solicitud", "Adjunto encontrarás el PDF con los detalles de tu solicitud.", pdfOutputStream.toByteArray(), archivoGuardado);

            } catch (Exception e) {
                System.out.println(e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al procesar el archivo " + e.getMessage());
            }

        // Convertir la solicitud guardada a JSON y devolverla
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        String requestJson;
        try {
            requestJson = mapper.writeValueAsString(savedRequest);
            System.out.println(requestJson);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al convertir a JSON");
        }

        // Devolver respuesta HTTP con estado 201 (creado)
        return ResponseEntity.status(HttpStatus.CREATED).body(requestJson);
    }



    @GetMapping("/get")
    public List<RequestDTO> get() {
        return requestServices.getAll();
    }

    @PutMapping("/cancel/{id}")
    public ResponseEntity<RequestDTO> cancelarSolicitud(@PathVariable Long id) {
        Optional<RequestDTO> optionalRequest = requestServices.findById(id);
        if (optionalRequest.isPresent()) {
            RequestDTO request = optionalRequest.get();
            // Asignar el estado "CANCELADA" de la entidad RequestState

            Optional<RequestState> canceladoState = requestStateService.findByName("Cancelado");
            request.setRequestState(canceladoState.get());
            requestServices.save(request);
            return ResponseEntity.ok(request);
        }
        return ResponseEntity.ok().build();
    }

    @PutMapping("/rechazar/{id}")
    public ResponseEntity<RequestDTO> rechazarSolicitud(@PathVariable Long id) {
        Optional<RequestDTO> optionalRequest = requestServices.findById(id);
        if (optionalRequest.isPresent()) {
            RequestDTO request = optionalRequest.get();
            // Asignar el estado "CANCELADA" de la entidad RequestState

            Optional<RequestState> canceladoState = requestStateService.findByName("Rechazado");
            request.setRequestState(canceladoState.get());
            requestServices.save(request);
            return ResponseEntity.ok(request);
        }
        return ResponseEntity.ok().build();
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestPart("request") RequestDTO requestDTO, @RequestPart(value = "archivo", required = false) MultipartFile archivo) {
        Optional<RequestDTO> requestDTOOptional = requestServices.findById(id);
        if (requestDTOOptional.isPresent()) {
            RequestDTO existingRequest = requestDTOOptional.get();
            existingRequest.setRequestState(requestDTO.getRequestState());
            existingRequest.setAnswer(requestDTO.getAnswer());
            // Actualizar otros campos si es necesario
            String archivoGuardado = null;
            if (archivo != null && !archivo.isEmpty()) {
                try {

                    //Si la Carpeta no Existe se crea
                    //Files.createDirectories(fileStorageLocation);
                    Files.createDirectories(Paths.get(uploadDir));

                    // Guardar el archivo
                    String fileName = archivo.getOriginalFilename();
                    System.out.println(fileName);

                    // Generar un nombre único para el archivo (ejemplo con timestamp)
                    String uniqueFileName = System.currentTimeMillis() + "_" + fileName;
                    System.out.println(uniqueFileName);

                    Path targetLocation = Paths.get(uploadDir).resolve(uniqueFileName);
                    System.out.println(targetLocation);

                    Files.copy(archivo.getInputStream(), targetLocation);

                    // Establecer la URL del archivo
                    existingRequest.setEvidenceAnswer(targetLocation.toString());

                    archivoGuardado = targetLocation.toString();  // Guardamos la ruta del archivo para el adjunto

                } catch (IOException e) {
                    System.out.println(e);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al procesar el archivo " + e.getMessage());
                }
            }

            try{
                UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                // Buscamos el usuario en la base de datos
                User user = userRepository.findByUser(userDetails.getUsername());


                if (user == null) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Usuario no encontrado");
                }
                //Si la Carpeta no Existe se crea
                //Files.createDirectories(fileStorageLocation);
                Files.createDirectories(Paths.get(uploadDir));

                // Generar un nombre único para el archivo PDF
                String fileNameAnswer = "respuesta_" + existingRequest.getRadicado() + ".pdf";
                String uniqueFileNameAnswer = System.currentTimeMillis() + "_" + fileNameAnswer;
                Path targetLocationAnswer = Paths.get(uploadDir).resolve(uniqueFileNameAnswer);


                // Generar PDF con iText
                ByteArrayOutputStream pdfOutputStream = new ByteArrayOutputStream();

                // 1. Crear el documento PDF
                PdfWriter writer = new PdfWriter(pdfOutputStream);
                PdfDocument pdfDoc = new PdfDocument(writer);
                Document document = new Document(pdfDoc);

                // 2. Añadir contenido al PDF
                document.add(new Paragraph("Fecha: " + existingRequest.getDate()));
                document.add(new Paragraph(" "));
                document.add(new Paragraph(" "));
                document.add(new Paragraph("Usuario: " + user.getName()));
                document.add(new Paragraph(" "));
                document.add(new Paragraph("Tipo de Solicitud: " + existingRequest.getRequestType().getNameRequestType()));
                document.add(new Paragraph(" "));
                document.add(new Paragraph("Categoría: " + existingRequest.getCategory().getNameCategory()));
                document.add(new Paragraph(" "));
                document.add(new Paragraph("Detalle de la Respuesta: " ));
                document.add(new Paragraph( requestDTO.getAnswer()));

                document.close();


                // Guardar el archivo PDF en el servidor
                Files.write(targetLocationAnswer, pdfOutputStream.toByteArray());


                // Enviar el PDF por correo
                emailService.sendEmailWithPdf(user.getEmail(), "Respuesta de solicitud  de Solicitud con Radicado: "+ existingRequest.getRadicado(), "Adjunto encontrarás el PDF con los detalles de ña respuesta de tu solicitud.", pdfOutputStream.toByteArray(), archivoGuardado);
                existingRequest.setArchivoAnswer(targetLocationAnswer.toString());
            } catch (Exception e) {
                System.out.println(e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al procesar el archivo " + e.getMessage());
            }

            RequestDTO updatedRequestDTO = requestServices.save(existingRequest); // Guardar los cambios en la solicitud existente
            return ResponseEntity.ok(updatedRequestDTO);
        }
        return ResponseEntity.notFound().build();
    }

//    @PutMapping("/update/{id}")
//    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody RequestDTO requestDTO) {
//        Optional<RequestDTO> requestDTOOptional = requestServices.findById(id);
//        if (requestDTOOptional.isPresent()) {
//            RequestDTO existingRequest = requestDTOOptional.get();
//            existingRequest.setRequestState(requestDTO.getRequestState());
//            existingRequest.setAnswer(requestDTO.getAnswer());
//            // Actualizar otros campos si es necesario
//
//            RequestDTO updatedRequestDTO = requestServices.save(existingRequest); // Guardar los cambios en la solicitud existente
//            String archivoGuardado = null;
//            try{
//                UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
//                // Buscamos el usuario en la base de datos
//                User user = userRepository.findByUser(userDetails.getUsername());
//
//
//                if (user == null) {
//                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Usuario no encontrado");
//                }
//                // Generar PDF con iText
//                ByteArrayOutputStream pdfOutputStream = new ByteArrayOutputStream();
//
//                // 1. Crear el documento PDF
//                PdfWriter writer = new PdfWriter(pdfOutputStream);
//                PdfDocument pdfDoc = new PdfDocument(writer);
//                Document document = new Document(pdfDoc);
//
//                // 2. Añadir contenido al PDF
//                document.add(new Paragraph("Fecha: " + existingRequest.getDate()));
//                document.add(new Paragraph(" "));
//                document.add(new Paragraph(" "));
//                document.add(new Paragraph("Usuario: " + user.getName()));
//                document.add(new Paragraph(" "));
//                document.add(new Paragraph("Tipo de Solicitud: " + existingRequest.getRequestType().getNameRequestType()));
//                document.add(new Paragraph(" "));
//                document.add(new Paragraph("Categoría: " + existingRequest.getCategory().getNameCategory()));
//                document.add(new Paragraph(" "));
//                document.add(new Paragraph("Detalle de la Respuesta: " ));
//                document.add(new Paragraph( requestDTO.getAnswer()));
//
//                document.close();
//
//                // Enviar el PDF por correo
//                emailService.sendEmailWithPdf(user.getEmail(), "Detalle de Solicitud", "Adjunto encontrarás el PDF con los detalles de tu solicitud.", pdfOutputStream.toByteArray(), archivoGuardado);
//
//            } catch (Exception e) {
//                System.out.println(e);
//                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al procesar el archivo " + e.getMessage());
//            }
//            return ResponseEntity.ok(updatedRequestDTO);
//        }
//        return ResponseEntity.notFound().build();
//    }

    @GetMapping("/download/{filename}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String filename) {
        try {
            //Path filePath = fileStorageLocation.resolve(filename).normalize();
            Path filePath = Paths.get(uploadDir).resolve(filename);
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.valueOf(Files.probeContentType(filePath)))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}