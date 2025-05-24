package kyrs.isis3.controller;

import jakarta.transaction.Transactional;
import kyrs.isis3.model.Student;
import kyrs.isis3.model.StudentGroup;
import kyrs.isis3.model.TeachingMethod;
import kyrs.isis3.repository.StudentGroupRepository;
import kyrs.isis3.repository.StudentRepository;
import kyrs.isis3.repository.TeachingMethodRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class StudentController {
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private StudentGroupRepository studentGroupRepository;
    @Autowired
    private TeachingMethodRepository teachingMethodRepository;

    @GetMapping("/student")
    public String studentMain(Model model) {
        Iterable<Student> students = studentRepository.findAll();
        model.addAttribute("students", students);
        return "student-main";
    }


    @GetMapping("/student/add")
    public String studentAdd(Model model) {
        model.addAttribute("studentGroups", studentGroupRepository.findAll());
        model.addAttribute("teachingMethods", teachingMethodRepository.findAll());
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
        return "studentGroup-add";
    }

    @PostMapping("/studentGroup/add")
    public String addStudentGroup (@RequestParam String grade , Model model) {
        StudentGroup studentGroup = new StudentGroup(grade);
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
    @GetMapping("/{studentGroupId}/students")
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

    // Страница со студентами конкретной группы
    @GetMapping("/{teachingMethodId}/students")
    public String listStudentsInTeachingMethod(@PathVariable Long teachingMethodId, Model model) {
        TeachingMethod teachingMethod = teachingMethodRepository.findById(teachingMethodId)
                .orElseThrow(() -> new IllegalArgumentException("Метод обучения не найден"));
        model.addAttribute("teachingMethod", teachingMethod);
        model.addAttribute("students", teachingMethod.getStudents());
        return "teachingMethod-students";
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
                             @RequestParam Long teachingMethodId,
                             @RequestParam String fullName,
                             Model model) {
        StudentGroup studentGroup = studentGroupRepository.findById(studentGroupId).orElseThrow();
        TeachingMethod teachingMethod = teachingMethodRepository.findById(teachingMethodId).orElseThrow();
        Student student = new Student(fullName, studentGroup, teachingMethod);
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
        model.addAttribute("teachingMethods", teachingMethodRepository.findAll());
        return "student-edit";
    }

    @PostMapping("/student/{id}/edit")
    public String studentUpdate(@PathVariable(value = "id") long idstudent,
                                @RequestParam Long studentGroupId,
                                @RequestParam Long teachingMethodId,
                                @RequestParam String fullName) {
        Student student = studentRepository.findById(idstudent).orElseThrow();
        student.setFullName(fullName);
        StudentGroup studentGroup = studentGroupRepository.findById(studentGroupId).orElseThrow();
        TeachingMethod teachingMethod = teachingMethodRepository.findById(teachingMethodId).orElseThrow();
        student.setStudentGroup(studentGroup);
        student.setTeachingMethod(teachingMethod);
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


























}
