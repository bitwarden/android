protocol ILogger{
    func LogError(_ error: String)
}

public class LoggerHelper: ILogger{
    func LogError(_ error: String) {
        print(error)
    }
}
