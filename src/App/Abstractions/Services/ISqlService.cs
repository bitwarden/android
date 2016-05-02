using SQLite;

namespace Bit.App.Abstractions
{
    public interface ISqlService
    {
        SQLiteConnection GetConnection();
    }
}
