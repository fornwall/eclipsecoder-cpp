package net.fornwall.eclipsecoder.ccsupport;

import java.lang.reflect.Array;
import java.util.Set;
import java.util.TreeSet;

import net.fornwall.eclipsecoder.stats.CodeGenerator;
import net.fornwall.eclipsecoder.stats.ProblemStatement;

public class CCCodeGenerator extends CodeGenerator {

	/** For test code generation */
	private static String wrap(String variableName, boolean multiDimensional, boolean string) {
		if (multiDimensional) {
			return ";\n            writeTo(cout, " + variableName + ");\n            cout";
		} else if (string) {
			return " << '\"' << " + variableName + " << '\"'";
		} else {
			return " << " + variableName;
		}
	}

	public CCCodeGenerator(ProblemStatement problemStatement) {
		super(problemStatement);
	}

	private String getArrayValueString(Object[] values) {
		StringBuilder builder = new StringBuilder("{");
		for (int i = 0; i < values.length; i++) {
			if (i != 0)
				builder.append(", ");
			Object element = values[i];
			if (element instanceof String) {
				builder.append("\"" + element.toString().replaceAll("\\\\", "\\\\\\\\") + "\"");
			} else {
				builder.append(element.toString());
			}
		}
		builder.append("}");
		return builder.toString();
	}

	private String getCreateStatement(Object value, String variableName) {
		Class<?> type = value.getClass();
		String description = getTypeString(type);
		if (type.isArray()) {
			if (Array.getLength(value) == 0) {
				return "        " + description + " " + variableName + ";\n";
			}
			return "        " + getTypeString(type.getComponentType()) + " " + variableName + "_[] = "
					+ getArrayValueString((Object[]) value) + ";\n" + "        " + description + " " + variableName
					+ "(" + variableName + "_, " + variableName + "_ + (sizeof(" + variableName + "_) / sizeof("
					+ variableName + "_[0])));\n";
		}

		if (value.getClass() == Long.class) {
			return "\t\t" + description + " " + variableName + " = " + value + "LL;\n";
		} else if (value.getClass() == String.class) {
			return "\t\t" + description + " " + variableName + " = \"" + ((String) value) + "\";\n";
		} else if (value.getClass() == Character.class) {
			char c = (Character) value;
			String escapedValue;
			switch (c) {
			case '\'':
				escapedValue = "\\'";
				break;
			case '\\':
				escapedValue = "\\\\";
				break;
			default:
				escapedValue = Character.toString(c);
			}
			return "\t\t" + description + " " + variableName + " = '" + escapedValue + "';\n";
		} else {
			return "\t\t" + description + " " + variableName + " = " + value + ";\n";
		}
	}

	@Override
	public String getDummyReturnString() {
		String type = getTypeString(problemStatement.getReturnType());
		if (type.equals("long long")) {
			return "0LL";
		}

		return type + "()";
	}

	@Override
	public String getTestsSource() {
		String testClassName = problemStatement.getSolutionClassName() + "Test";
		Class<?> returnType = problemStatement.getReturnType();
		String returnTypeName = getTypeString(returnType);
		boolean multiDimensional = returnType.isArray();
		boolean string = returnTypeName.equals("string");

		StringBuilder buffer = new StringBuilder();
		buffer.append("#include \"" + problemStatement.getSolutionClassName() + ".h\"\n");

		// Only add those include files that are needed
		Set<String> includeFiles = new TreeSet<String>();
		includeFiles.add("#include <iostream>\n");
		for (Class<?> parameterType : problemStatement.getParameterTypes()) {
			if (parameterType == String.class) {
				includeFiles.add("#include <string>\n");
			}
			if (parameterType.isArray()) {
				includeFiles.add("#include <vector>\n");
			}
		}
		if (returnTypeName.contains("string"))
			includeFiles.add("#include <string>\n");
		if (returnType.isArray())
			includeFiles.add("#include <vector>\n");
		if (returnTypeName.contains("double")) {
			// std::max and std::abs
			buffer.append("#include <algorithm>\n");
			buffer.append("#include <cmath>\n");
		}

		for (String s : includeFiles)
			buffer.append(s);

		buffer.append("\nusing std::cerr;\nusing std::cout;\nusing std::endl;\n");
		if (returnTypeName.contains("double")) {
			includeFiles.add("using std::abs\n");
			includeFiles.add("using std::max\n");
		}
		if (includeFiles.contains("#include <string>\n"))
			buffer.append("using std::string;\n");
		if (includeFiles.contains("#include <vector>\n"))
			buffer.append("using std::vector;\n");

		buffer.append("\nclass " + testClassName + " {\n\n");

		if (multiDimensional) {
			buffer.append("    static void writeTo(std::ostream& out, const " + returnTypeName + "& v) {\n"
					+ "        out << '{';\n" + "        for (unsigned int i = 0; i < v.size(); i++) {\n");
			if (returnTypeName.indexOf("string") > 0) {
				buffer.append("            out << '\"' << v[i] << '\"';\n");
			} else {
				buffer.append("            out << v[i];\n");
			}
			buffer.append("            if (i + 1 != v.size()) out << \", \";\n" + "        }\n"
					+ "        out << '}';\n" + "    }\n\n");
		}

		if (returnTypeName.equalsIgnoreCase("double")) {
			buffer
					.append("    static void assertEquals(int testCase, double expected, double actual) {\n"
							+ "        double delta = max(1e-9, 1e-9 * abs(expected));\n"
							+ "        if (abs(expected - actual) <= delta) {\n"
							+ "            cout << \"Test case \" << testCase << \" PASSED!\" << endl;\n"
							+ "        } else {\n"
							+ "            cout.precision(24);\n"
							+ "            cout << \"Test case \" << testCase << \" FAILED! Expected: <\" << expected << \"> but was: <\" << actual << '>' << endl;\n"
							+ "        }\n" + "    }\n" + "\n");
		} else if (returnTypeName.equalsIgnoreCase("vector <double>")) {
			buffer
					.append("    static void assertEquals(int testCase, const vector<double>& expected, const vector<double>& actual) {\n"
							+ "        bool failed = expected.size() != actual.size();\n"
							+ "        for (unsigned int i = 0; i < expected.size() && !failed; i++) {\n"
							+ "            double delta = max(1e-9, 1e-9 * abs(expected[i]));\n"
							+ "            failed = (abs(expected[i] - actual[i]) > delta);\n"
							+ "        }\n"
							+ "        if (failed) {\n"
							+ "            cout.precision(24);\n"
							+ "            cout << \"Test case \" << testCase << \" FAILED! Expected: <\""
							+ wrap("expected", multiDimensional, string)
							+ " << \"> but was: <\""
							+ wrap("actual", multiDimensional, string)
							+ " << '>' << endl;\n"
							+ "        } else {\n"
							+ "            cout << \"Test case \" << testCase << \" PASSED!\" << endl;\n"
							+ "        }\n" + "    }\n" + "\n");
		} else {
			buffer.append("    static void assertEquals(int testCase, const "
					+ returnTypeName
					+ "& expected, const "
					+ returnTypeName
					+ "& actual) {\n"
					+ "        if (expected == actual) {\n"
					+ "            cout << \"Test case \" << testCase << \" PASSED!\" << endl;\n"
					+ "        } else {\n"
					// NOTE: If one mixes output to stderr and stdout
					// (cerr and cout), the eclipse console does
					// not guarantee the order between the output
					// (possibly changed in ecilpse 3.1). Otherwise
					// one would probably want stderr on test failure.
					+ "            cout << \"Test case \" << testCase << \" FAILED! Expected: <\""
					+ wrap("expected", multiDimensional, string) + " << \"> but was: <\""
					+ wrap("actual", multiDimensional, string) + " << '>' << endl;\n" + "        }\n" + "    }\n"
					+ "\n");
		}

		buffer.append("    " + problemStatement.getSolutionClassName() + " solution;\n" + "\n");

		for (int i = 0; i < problemStatement.getTestCases().size(); i++) {
			ProblemStatement.TestCase testCase = problemStatement.getTestCases().get(i);
			buffer.append("    void testCase" + i + "() {\n");

			for (int paramNumber = 0; paramNumber < testCase.getParameters().length; paramNumber++) {
				buffer.append(getCreateStatement(testCase.getParameters()[paramNumber], problemStatement
						.getParameterNames().get(paramNumber)));
			}

			buffer.append(getCreateStatement(testCase.getReturnValue(), "expected_"));

			buffer.append("        assertEquals(" + i + ", expected_, solution."
					+ problemStatement.getSolutionMethodName() + "(");
			for (int param = 0; param < problemStatement.getParameterNames().size(); param++) {
				if (param != 0)
					buffer.append(", ");
				buffer.append(problemStatement.getParameterNames().get(param));
			}

			buffer.append("));\n    }\n\n");
		}

		buffer.append("    public: void runTest(int testCase) {\n" + "        switch (testCase) {\n");
		for (int i = 0; i < problemStatement.getTestCases().size(); i++) {
			buffer.append("            case (" + i + "): testCase" + i + "(); break;\n");
		}
		buffer.append("            default: cerr << \"No such test case: \" << testCase << endl; break;\n");

		buffer.append("        }\n" + "    }\n\n" + "};\n" + "\n" + "int main() {\n" + "    for (int i = 0; i < "
				+ problemStatement.getTestCases().size() + "; i++) {\n" + "        " + testClassName + " test;\n"
				+ "        test.runTest(i);\n" + "    }\n" + "}\n");
		return buffer.toString();
	}

	@Override
	public String getTypeString(Class<?> type) {
		if (type == Integer.class) {
			return "int";
		} else if (type == Character.class) {
			return "char";
		} else if (type == Long.class) {
			return "long long";
		} else if (type == Double.class) {
			return "double";
		} else if (type == String.class) {
			return "string";
		} else if (type.isArray()) {
			return "vector<" + getTypeString(type.getComponentType()) + ">";
		} else {
			throw new IllegalArgumentException("Got type \"" + type.getName() + "\"");
		}
	}

}
