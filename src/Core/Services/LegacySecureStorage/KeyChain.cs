﻿#if IOS

using Foundation;
using Security;

namespace Bit.Core.Services;

class KeyChain
{
    SecAccessible accessible;

    internal KeyChain(SecAccessible accessible) =>
        this.accessible = accessible;

    SecRecord ExistingRecordForKey(string key, string service)
    {
        return new SecRecord(SecKind.GenericPassword)
        {
            Account = key,
            Service = service
        };
    }

    internal string ValueForKey(string key, string service)
    {
        using (var record = ExistingRecordForKey(key, service))
        using (var match = SecKeyChain.QueryAsRecord(record, out var resultCode))
        {
            if (resultCode == SecStatusCode.Success)
                return NSString.FromData(match.ValueData, NSStringEncoding.UTF8);
            else
                return null;
        }
    }

    internal void SetValueForKey(string value, string key, string service)
    {
        using (var record = ExistingRecordForKey(key, service))
        {
            if (string.IsNullOrEmpty(value))
            {
                if (!string.IsNullOrEmpty(ValueForKey(key, service)))
                    RemoveRecord(record);

                return;
            }

            // if the key already exists, remove it
            if (!string.IsNullOrEmpty(ValueForKey(key, service)))
                RemoveRecord(record);
        }

        using (var newRecord = CreateRecordForNewKeyValue(key, value, service))
        {
            var result = SecKeyChain.Add(newRecord);

            switch (result)
            {
                case SecStatusCode.DuplicateItem:
                    {
                        Debug.WriteLine("Duplicate item found. Attempting to remove and add again.");

                        // try to remove and add again
                        if (Remove(key, service))
                        {
                            result = SecKeyChain.Add(newRecord);
                            if (result != SecStatusCode.Success)
                                throw new Exception($"Error adding record: {result}");
                        }
                        else
                        {
                            Debug.WriteLine("Unable to remove key.");
                        }
                    }
                    break;
                case SecStatusCode.Success:
                    return;
                default:
                    throw new Exception($"Error adding record: {result}");
            }
        }
    }

    internal bool Remove(string key, string service)
    {
        using (var record = ExistingRecordForKey(key, service))
        using (var match = SecKeyChain.QueryAsRecord(record, out var resultCode))
        {
            if (resultCode == SecStatusCode.Success)
            {
                RemoveRecord(record);
                return true;
            }
        }
        return false;
    }

    internal void RemoveAll(string service)
    {
        using (var query = new SecRecord(SecKind.GenericPassword) { Service = service })
        {
            SecKeyChain.Remove(query);
        }
    }

    SecRecord CreateRecordForNewKeyValue(string key, string value, string service)
    {
        return new SecRecord(SecKind.GenericPassword)
        {
            Account = key,
            Service = service,
            Label = key,
            Accessible = accessible,
            ValueData = NSData.FromString(value, NSStringEncoding.UTF8),
        };
    }

    bool RemoveRecord(SecRecord record)
    {
        var result = SecKeyChain.Remove(record);
        if (result != SecStatusCode.Success && result != SecStatusCode.ItemNotFound)
            throw new Exception($"Error removing record: {result}");

        return true;
    }
}
#endif
