package joelbits.modules.analysis.plugins.mappers;

import joelbits.modules.analysis.plugins.utils.AnalysisUtil;
import joelbits.model.ast.ASTRoot;
import joelbits.model.project.CodeRepository;
import joelbits.model.project.Project;
import joelbits.modules.analysis.plugins.visitors.BenchmarkCountVisitor;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public final class BenchmarkCountMapper extends Mapper<Text, BytesWritable, Text, Text> {
    private final List<String> processedBenchmarkFiles = new ArrayList<>();
    private final AnalysisUtil analysisUtil = new AnalysisUtil();

    @Override
    public void map(Text key, BytesWritable value, Context context) throws IOException, InterruptedException {
        int nrOfBenchmarks = 0;
        Project project = analysisUtil.getProject(Arrays.copyOf(value.getBytes(), value.getLength()));
        BenchmarkCountVisitor visitor = new BenchmarkCountVisitor();
        for (CodeRepository repository : project.getRepositories()) {
            Set<ASTRoot> benchmarkFiles = analysisUtil.latestFileSnapshots(repository);

            for (ASTRoot changedFile : benchmarkFiles) {
                visitor.resetSum();
                String declarationName = changedFile.getNamespaces().get(0).getDeclarations().get(0).getName();
                if (processedBenchmarkFiles.contains(declarationName)) {
                    continue;
                }
                processedBenchmarkFiles.add(declarationName);

                changedFile.accept(visitor);
                nrOfBenchmarks += visitor.getSum();
                context.write(new Text(project.getName() + ":" + declarationName), new Text(Integer.toString(visitor.getSum())));
            }
        }
        context.write(new Text(project.getName()), new Text(Integer.toString(nrOfBenchmarks)));
    }
}
