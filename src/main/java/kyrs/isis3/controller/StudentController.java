package kyrs.isis3.controller;

import jakarta.transaction.Transactional;
import kyrs.isis3.model.Grade;
import kyrs.isis3.model.Student;
import kyrs.isis3.model.StudentGroup;
import kyrs.isis3.model.TeachingMethod;
import kyrs.isis3.repository.GradeRepository;
import kyrs.isis3.repository.StudentGroupRepository;
import kyrs.isis3.repository.StudentRepository;
import kyrs.isis3.repository.TeachingMethodRepository;
import kyrs.isis3.service.GradeService;
import kyrs.isis3.service.StudentGroupService;
import kyrs.isis3.service.StudentService;
import kyrs.isis3.service.TeachingMethodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Controller
public class StudentController {
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private StudentGroupRepository studentGroupRepository;
    @Autowired
    private TeachingMethodRepository teachingMethodRepository;
    @Autowired
    private StudentService studentService;
    @Autowired
    private StudentGroupService studentGroupService;
    @Autowired
    private TeachingMethodService teachingMethodService;
    @Autowired
    private GradeService gradeService;
    @Autowired
    private GradeRepository gradeRepository;

    @GetMapping("/student/add")
    public String studentAdd(Model model) {
        model.addAttribute("studentGroups", studentGroupRepository.findAll());
        return "student-add";
    }

    @GetMapping("/studentAnon")
    public String studentsMainAnon(Model model) {
        model.addAttribute("title", "Студенты");
        Iterable<Student> students = studentRepository.findAll();
        model.addAttribute("students", students);
        return "Stanon";
    }

    @GetMapping("/studentGroup/add")
    public String studentGroupAdd(Model model) {
        model.addAttribute("teachingMethods", teachingMethodRepository.findAll());
        return "studentGroup-add";
    }

    @PostMapping("/studentGroup/add")
    public String addStudentGroup (@RequestParam String nameGroup,
                                   @RequestParam Long teachingMethodId, Model model) {
        TeachingMethod teachingMethod = teachingMethodRepository.findById(teachingMethodId).orElseThrow();
        StudentGroup studentGroup = new StudentGroup(nameGroup, teachingMethod);
        studentGroupRepository.save(studentGroup);
        return "redirect:/studentGroup";
    }

    // Страница со списком всех групп
    @GetMapping("/studentGroup")
    public String listGroups(Model model) {
        List<StudentGroup> studentGroups = studentGroupRepository.findAll();
        model.addAttribute("studentGroups", studentGroups);
        return "studentGroup-list"; // имя HTML-шаблона
    }

    // Страница со студентами конкретной группы
    @GetMapping("/studentGroup/{studentGroupId}")
    public String listStudentsInGroup(@PathVariable Long studentGroupId, Model model) {
        StudentGroup studentGroup = studentGroupRepository.findById(studentGroupId)
                .orElseThrow(() -> new IllegalArgumentException("Группа не найдена"));
        model.addAttribute("studentGroup", studentGroup);
        model.addAttribute("students", studentGroup.getStudents());
        return "studentGroup-students";
    }

    // Страница со списком всех методов обучения
    @GetMapping("/teachingMethod")
    public String listTeachingMethods(Model model) {
        List<TeachingMethod> teachingMethods = teachingMethodRepository.findAll();
        model.addAttribute("teachingMethods", teachingMethods);
        return "teachingMethod-list"; // имя HTML-шаблона
    }

    @GetMapping("/teachingMethod/add")
    public String teachingMethodAdd(Model model) {
        return "teachingMethod-add";
    }

    @PostMapping("/teachingMethod/add")
    public String addTeachingMethod (@RequestParam String nameMethod , Model model) {
        TeachingMethod teachingMethod = new TeachingMethod(nameMethod);
        teachingMethodRepository.save(teachingMethod);
        return "redirect:/teachingMethod";
    }

    @Transactional
    @DeleteMapping("/teachingMethod/{id}")
    public String deleteTeachingMethodWithGroupsAndStudents(@PathVariable Long id) {
        Optional<TeachingMethod> teachingMethodOptional = teachingMethodRepository.findById(id);
        if (teachingMethodOptional.isPresent()) {
            TeachingMethod teachingMethod = teachingMethodOptional.get();

            // Получаем все группы этого метода
            List<StudentGroup> groups = studentGroupRepository.findByTeachingMethod(teachingMethod);

            // Для каждой группы удаляем студентов и их оценки
            for (StudentGroup group : groups) {
                // Удаляем оценки студентов этой группы
                gradeRepository.deleteByStudent_StudentGroup(group);

                // Удаляем студентов этой группы
                studentRepository.deleteByStudentGroup(group);

                // Удаляем саму группу
                studentGroupRepository.delete(group);
            }

            // Удаляем сам метод обучения
            teachingMethodRepository.delete(teachingMethod);
        }
        return "redirect:/teachingMethod";
    }

    @PostMapping("/student/add")
    public String addStudent(@RequestParam Long studentGroupId,
                             @RequestParam String fullName,
                             Model model) {
        StudentGroup studentGroup = studentGroupRepository.findById(studentGroupId).orElseThrow();
        Student student = new Student(fullName, studentGroup);
        studentRepository.save(student);
        return "redirect:/student";
    }

    @GetMapping("/student/{id}")
    public String studentDetails(@PathVariable(value = "id") long id, Model model) {
        Student student = studentRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Неверный идентификатор студента: " + id));
        model.addAttribute("student", student);
        return "student-details";
    }

    @GetMapping("/student/{id}/edit")
    public String studentEdit(@PathVariable(value = "id") long idstudent, Model model) {
        Student student = studentRepository.findById(idstudent).orElseThrow();
        model.addAttribute("student", student);
        model.addAttribute("studentGroups", studentGroupRepository.findAll());
        return "student-edit";
    }

    @PostMapping("/student/{id}/edit")
    public String studentUpdate(@PathVariable(value = "id") long idstudent,
                                @RequestParam Long studentGroupId,
                                @RequestParam String fullName) {
        Student student = studentRepository.findById(idstudent).orElseThrow();
        student.setFullName(fullName);
        StudentGroup studentGroup = studentGroupRepository.findById(studentGroupId).orElseThrow();
        student.setStudentGroup(studentGroup);
        studentRepository.save(student);
        return "redirect:/student/" + idstudent;
    }

    @Transactional
    @PostMapping("/student/{id}/delete")
    public String studentDelete(@PathVariable(value = "id") long idStudent) {
        Student student = studentRepository.findById(idStudent)
                .orElseThrow(() -> new IllegalArgumentException("Студент не найден"));

        // Удаляем все оценки студента
        gradeRepository.deleteByStudent(student);

        // Удаляем самого студента
        studentRepository.delete(student);

        return "redirect:/student";
    }

    @GetMapping("/studentGroup/{studentGroupId}/students")
    public String showStudentsByGroup(@PathVariable Long studentGroupId, Model model) {
        Optional<StudentGroup> studentGroup = studentGroupService.getStudentGroupById(studentGroupId);
        if (studentGroup.isPresent()) {
            model.addAttribute("studentGroup", studentGroup.get());
            model.addAttribute("students", studentService.getStudentsByGroupId(studentGroupId));
            return "studentGroup-students";
        }
        return "redirect:/studentGroup";
    }

    @GetMapping("/teachingMethod/{teachingMethodId}/groups")
    public String showGroupsByMethod(@PathVariable Long teachingMethodId, Model model) {
        Optional<TeachingMethod> teachingMethod = teachingMethodService.getTeachingMethodById(teachingMethodId);
        if (teachingMethod.isPresent()) {
            model.addAttribute("teachingMethod", teachingMethod.get());
            model.addAttribute("studentGroups", studentGroupService.getGroupsByTeachingMethodId(teachingMethodId));
            return "teachingMethod-groups";
        }
        return "redirect:/teachingMethod";
    }

    @GetMapping("/student/{studentId}/grades")
    public String showStudentGrades(@PathVariable Long studentId, Model model) {
        Optional<Student> studentOptional = studentRepository.findById(studentId);
        if (studentOptional.isEmpty()) {
            return "redirect:/student";
        }
        Student student = studentOptional.get();
        model.addAttribute("student", student);
        model.addAttribute("grades", gradeService.getGradesByStudent(Optional.of(student)));
        return "student-grades";
    }

    @GetMapping("/student/{studentId}/grades/add")
    public String showAddGradeForm(@PathVariable Long studentId, Model model) {
        Optional<Student> studentOptional = studentRepository.findById(studentId);
        if (studentOptional.isEmpty()) {
            return "redirect:/student";
        }
        Student student = studentOptional.get();
        model.addAttribute("student", student);
        model.addAttribute("grade", new Grade());
        return "grade-add";
    }

    @PostMapping("/student/{studentId}/grades/add")
    public String addGrade(@PathVariable Long studentId,
                           @ModelAttribute Grade grade) {
        Optional<Student> studentOptional = studentRepository.findById(studentId);
        if (studentOptional.isPresent()) {
            Student student = studentOptional.get();
            grade.setStudent(student);
            gradeService.saveGrade(grade);
        }
        return "redirect:/student/" + studentId + "/grades";
    }

    @GetMapping("/studentGroup/{studentGroupId}/grades")
    public String showGroupGrades(@PathVariable Long studentGroupId, Model model) {
        StudentGroup studentGroup = studentGroupService.getStudentGroupById(studentGroupId).orElse(null);
        if (studentGroup == null) {
            return "redirect:/studentGroup";
        }

        // Получаем студентов с их оценками
        List<Student> students = studentRepository.findByStudentGroupIdStudentGroupWithGrades(studentGroupId);

        // Собираем уникальные даты всех оценок
        Set<LocalDate> distinctDates = students.stream()
                .flatMap(s -> s.getGrades().stream())
                .map(Grade::getTestDate)
                .sorted()
                .collect(Collectors.toCollection(LinkedHashSet::new));

        model.addAttribute("studentGroup", studentGroup);
        model.addAttribute("studentsWithGrades", students);
        model.addAttribute("distinctDates", distinctDates);
        return "group-grades";
    }

    @DeleteMapping("/studentGroup/{id}")
    public String deleteGroupWithStudents(@PathVariable Long id) {
        studentGroupService.deleteGroupWithStudents(id);
        return "redirect:/student";
    }

    @PostMapping("/student/grades/{gradeId}")
    public String deleteGrade(@PathVariable Long gradeId,
                              @RequestParam("studentId") Long studentId) {
        gradeService.deleteGrade(gradeId);
        return "redirect:/student/" + studentId + "/grades";
    }

    @GetMapping("/student")
    public String studentMain(@RequestParam(required = false) Long studentGroupId,
                              @RequestParam(required = false) Long teachingMethodId,
                              Model model) {
        model.addAttribute("students", studentService.filterStudents(studentGroupId, teachingMethodId));
        model.addAttribute("studentGroups", studentService.getAllGroups());
        model.addAttribute("teachingMethods", studentService.getAllMethods());
        model.addAttribute("selectedGroup", studentGroupId);
        model.addAttribute("selectedMethod", teachingMethodId);
        return "student-main";
    }


    ////////////////
    @GetMapping("/student/upload")
    public String showUploadForm(Model model) {
        model.addAttribute("studentGroups", studentGroupRepository.findAll());
        return "student-upload";
    }

    @PostMapping("/student/upload")
    public String uploadStudents(@RequestParam("file") MultipartFile file,
                                 @RequestParam Long studentGroupId,
                                 RedirectAttributes redirectAttributes) {

        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("message", "Пожалуйста, выберите файл для загрузки");
            return "redirect:/student/upload";
        }

        try {
            StudentGroup group = studentGroupRepository.findById(studentGroupId)
                    .orElseThrow(() -> new IllegalArgumentException("Группа не найдена"));

            InputStream inputStream = file.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            // Пропускаем заголовок, если есть
            reader.readLine();

            String line;
            int count = 0;
            while ((line = reader.readLine()) != null) {
                String[] data = line.split(",");
                if (data.length >= 1) {
                    String fullName = data[0].trim();
                    if (!fullName.isEmpty()) {
                        Student student = new Student(fullName, group);
                        studentRepository.save(student);
                        count++;
                    }
                }
            }

            redirectAttributes.addFlashAttribute("message",
                    "Успешно загружено " + count + " студентов в группу " + group.getNameGroup());

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message",
                    "Ошибка при загрузке файла: " + e.getMessage());
        }

        return "redirect:/student";
    }



    ////
    @GetMapping("/all/upload")
    public String showUpload2Form(Model model) {
        model.addAttribute("teachingMethods", teachingMethodRepository.findAll());
        return "all-upload";
    }

    @PostMapping("/all/upload")
    public String uploadStudents(@RequestParam("file") MultipartFile file,
                                 RedirectAttributes redirectAttributes) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Пожалуйста, выберите файл для загрузки");
            return "redirect:/all/upload";
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            // Пропускаем заголовок
            reader.readLine();

            String line;
            AtomicInteger studentCount = new AtomicInteger();
            AtomicInteger gradeUpdatedCount = new AtomicInteger(0);
            AtomicInteger gradeCount = new AtomicInteger(0);
            List<String> validationErrors = new ArrayList<>();

            while ((line = reader.readLine()) != null) {
                try {
                    String[] data = line.split(",");

                    if (data.length != 6) {
                        validationErrors.add("Некорректное количество полей в строке: " + line);
                        continue;
                    }

                    // Валидация ФИО (только буквы, пробелы, дефисы и специальные символы, но без цифр)
                    String fullName = data[0].trim().replaceAll("^\"|\"$", "");
                    if (!fullName.matches("^[\\p{L} -]+$")) {
                        validationErrors.add("Некорректное ФИО: " + fullName + " (допустимы только буквы, пробелы и дефисы)");
                        continue;
                    }

                    String groupName = data[1].trim().replaceAll("^\"|\"$.,", "");

                    // Валидация метода обучения
                    String methodName = data[2].trim().replaceAll("^\"|\"$", "");

                    // Валидация оценки (только 2, 3, 4 или 5)
                    String scoreStr = data[3].trim().replaceAll("^\"|\"$", "");
                    if (!scoreStr.matches("[2-5]")) {
                        validationErrors.add("Некорректная оценка: " + scoreStr);
                        continue;
                    }
                    double score = Double.parseDouble(scoreStr);

                    // Валидация даты (только цифры и разделители - или .)
                    String dateStr = data[4].trim().replaceAll("^\"|\"$", "");
                    if (!dateStr.matches("^\\d{4}[-.]\\d{2}[-.]\\d{2}$")) {
                        validationErrors.add("Некорректный формат даты: " + dateStr);
                        continue;
                    }
                    // Заменяем точки на дефисы для парсинга
                    dateStr = dateStr.replace('.', '-');
                    LocalDate testDate = LocalDate.parse(dateStr);

                    String testName = data[5].trim().replaceAll("^\"|\"$", "");

                    // Поиск метода обучения
                    TeachingMethod method = teachingMethodRepository.findByNameMethod(methodName)
                            .orElseGet(() -> {
                                TeachingMethod newMethod = new TeachingMethod(methodName);
                                return teachingMethodRepository.save(newMethod);
                            });

                    // Поиск или создание группы
                    StudentGroup group = studentGroupRepository.findByNameGroup(groupName)
                            .orElseGet(() -> {
                                StudentGroup newGroup = new StudentGroup(groupName, method);
                                return studentGroupRepository.save(newGroup);
                            });

                    // Поиск или создание студента
                    Student student = studentRepository.findByFullNameAndStudentGroup(fullName, group)
                            .orElseGet(() -> {
                                Student newStudent = new Student(fullName, group);
                                studentRepository.save(newStudent);
                                studentCount.getAndIncrement();
                                return newStudent;
                            });

                    // Обработка оценки
                    Optional<Grade> existingGrade = gradeRepository.findByStudentAndTestNameAndTestDate(
                            student, testName, testDate);

                    if (existingGrade.isPresent()) {
                        Grade grade = existingGrade.get();
                        grade.setValueScore(String.valueOf(score));
                        gradeRepository.save(grade);
                        gradeUpdatedCount.incrementAndGet();
                    } else {
                        Grade grade = new Grade();
                        grade.setStudent(student);
                        grade.setValueScore(String.valueOf(score));
                        grade.setTestDate(testDate);
                        grade.setTestName(testName);
                        gradeRepository.save(grade);
                        gradeCount.incrementAndGet();
                    }

                } catch (Exception e) {
                    validationErrors.add("Ошибка обработки строки: " + line + " - " + e.getMessage());
                }
            }

            if (!validationErrors.isEmpty()) {
                redirectAttributes.addFlashAttribute("validationErrors", validationErrors);
            }

            redirectAttributes.addFlashAttribute("success",
                    String.format("Успешно загружено: %d студентов и %d оценок",
                            studentCount.get(), gradeCount.get()));

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Ошибка при обработке файла: " + e.getMessage());
        }

        return "redirect:/all/upload";
    }
}
