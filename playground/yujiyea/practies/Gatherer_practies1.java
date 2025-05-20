import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Gatherer;

public class Gatherer_practies1 {
    public static void main(String[] args) {
        List<String> strList = List.of("1","11","111","222","33","666666","4444", "5555");
        
        Gatherer<String, Set<Integer>, String> gather = Gatherer.of(
            HashSet::new, 
            (seenLengths, str, downStream) ->{
                if(seenLengths.add(str.length())){
                    // downStream.push(str);
                    // return false;
                }
                return true;
            },
            (left, right) -> {
                System.out.println("Combining states"); // 확인용 로그
                left.addAll(right); 
                return left;
            },
            (seenLengths, downStream) -> {}
        );
        
        strList.stream()
            .gather(gather)
            .forEach(System.out::println);
            
        System.out.println("병렬 스트림림");
        strList.parallelStream()
            .gather(gather)
            .forEach(System.out::println);
    }
}
