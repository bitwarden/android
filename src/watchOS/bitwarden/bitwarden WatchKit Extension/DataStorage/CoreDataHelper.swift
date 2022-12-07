import Foundation
import CoreData

// Based on https://medium.com/swlh/using-core-data-in-your-swiftui-app-with-combine-mvvm-and-protocols-4577f44d240d

class CoreDataHelper: DBHelperProtocol {
    static let shared = CoreDataHelper()
    
    typealias ObjectType = NSManagedObject
    typealias PredicateType = NSPredicate
    
    var context: NSManagedObjectContext { persistentContainer.viewContext }
    
    // MARK: - Core Data
    
    lazy var persistentContainer: NSPersistentContainer = {
        StringEncryptionTransformer.register()
        let container = NSPersistentContainer(name: "BitwardenDB")

        container.loadPersistentStores(completionHandler: { (storeDescription, error) in
            if let error = error as NSError? {
                fatalError("Unresolved error \(error), \(error.userInfo)")
            }
        })
        return container
    }()

    func saveContext () {
        let context = persistentContainer.viewContext
        if context.hasChanges {
            do {
                try context.save()
            } catch {
                let nserror = error as NSError
                fatalError("Unresolved error \(nserror), \(nserror.userInfo)")
            }
        }
    }
    
    // MARK: -  DBHelper Protocol
    
    
    func create(_ object: NSManagedObject) {
        do {
            try context.save()
        } catch {
            fatalError("error saving context while creating an object")
        }
    }
    
    func fetch<T: NSManagedObject>(_ objectType: T.Type, _ entityName: String, predicate: NSPredicate? = nil, limit: Int? = nil) -> Result<[T], Error> {
        let request = NSFetchRequest<T>(entityName: entityName)
        request.predicate = predicate
        if let limit = limit {
            request.fetchLimit = limit
        }
        do {
            let result = try context.fetch(request)
            return .success(result as [T])
        } catch {
            return .failure(error)
        }
    }
    
    func fetchFirst<T: NSManagedObject>(_ objectType: T.Type, predicate: NSPredicate?) -> Result<T?, Error> {
        let result = fetch(objectType, predicate: predicate, limit: 1)
        switch result {
        case .success(let todos):
            return .success(todos.first as? T)
        case .failure(let error):
            return .failure(error)
        }
    }
    
    func update(_ object: NSManagedObject) {
        do {
            try context.save()
        } catch {
            fatalError("error saving context while updating an object")
        }
    }
    
    func delete(_ object: NSManagedObject) {
        context.delete(object)
    }
    
    func insertBatch(_ entityName: String, items: [Any], itemMapper: @escaping (Any, NSManagedObjectContext) -> [String : Any], completionHandler: @escaping () -> Void) {
        self.persistentContainer.performBackgroundTask { context in
            context.mergePolicy = NSMergeByPropertyObjectTrumpMergePolicy
            let objects = items.map { item in
                itemMapper(item, context)
            }
            let batchInsert = NSBatchInsertRequest(entityName: entityName, objects: objects)
            batchInsert.resultType = NSBatchInsertRequestResultType.objectIDs
            do {
                let result = try context.execute(batchInsert) as! NSBatchInsertResult
                if let objectIDs = result.result as? [NSManagedObjectID], !objectIDs.isEmpty {
                    let save = [NSInsertedObjectsKey: objectIDs]
                    NSManagedObjectContext.mergeChanges(fromRemoteContextSave: save, into: [self.context])
                }
            }
            catch let nsError as NSError {
                fatalError("Unresolved error \(nsError), \(nsError.userInfo)")
            }
            DispatchQueue.main.async {
                completionHandler()
            }
        }
    }
    
    func deleteAll(_ entityName: String, predicate: NSPredicate? = nil, completionHandler: @escaping () -> Void) {
        let fetchRequest: NSFetchRequest<NSFetchRequestResult> = NSFetchRequest(entityName: entityName)
        fetchRequest.predicate = predicate
        let deleteRequest = NSBatchDeleteRequest(fetchRequest: fetchRequest)
        deleteRequest.resultType = .resultTypeObjectIDs

        self.persistentContainer.performBackgroundTask { context in
            do {
                try context.execute(deleteRequest)
            } catch let nsError as NSError {
                Log.e("Unresolved error \(nsError), \(nsError.userInfo)")
            }

            completionHandler()
        }
    }
}
