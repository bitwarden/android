using Bit.App.Abstractions;
using SQLite;

namespace Bit.App.Repositories
{
    public abstract class BaseRepository
    {
        public BaseRepository(ISqlService sqlService)
        {
            Connection = sqlService.GetConnection();
        }

        protected SQLiteConnection Connection { get; private set; }
    }
}
