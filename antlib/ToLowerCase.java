public class ToLowerCase extends org.apache.tools.ant.Task {
    String value;
    public void setValue(String s)
    {
        value = s;
    }

    String target;
    public void setTarget(String s)
    {
        target = s;
    }

    @Override
    public void execute() {
        getProject().setNewProperty(target, value.toLowerCase());
    }
}
