package kyrs.isis3.controller;

import jakarta.transaction.Transactional;
import kyrs.isis3.model.Grade;
import kyrs.isis3.model.Student;
import kyrs.isis3.model.StudentGroup;
import kyrs.isis3.model.TeachingMethod;
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
import java.util.LinkedHashSet;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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

//    @GetMapping("/student")
//    public String studentMain(Model model) {
//        Iterable<Student> students = studentRepository.findAll();
//        model.addAttribute("students", students);
//        return "student-main";
//    }


    @GetMapping("/student/add")
    public String studentAdd(Model model) {
        model.addAttribute("studentGroups", studentGroupRepository.findAll());
//        model.addAttribute("teachingMethods", teachingMethodRepository.findAll());
        return "student-add";
    }

    @GetMapping("/studentAnon")
    public String studentsMainAnon(Model model) {
        model.addAttribute("title", "Студенты");
        Iterable<Student> students = studentRepository.findAll();
        model.addAttribute("students", students);
        return "studentAnon";
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
        return "redirect:/student";
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
        return "redirect:/";
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
        Student student = studentRepository.findById(idStudent).orElseThrow();
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



    ///////////////////////////////////////////////////////////////
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

//    @GetMapping("/studentGroup/{studentGroupId}/grades")
//    public String showGroupGrades(@PathVariable Long studentGroupId, Model model) {
//        StudentGroup studentGroup = studentGroupService.getStudentGroupById(studentGroupId).orElse(null);
//        if (studentGroup == null) {
//            return "redirect:/studentGroup";
//        }
//        model.addAttribute("studentGroup", studentGroup);
//        model.addAttribute("grades", gradeService.getGradesByGroupId(studentGroupId));
//        return "group-grades";
//    }





    //////////////////////////////////////
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











}
