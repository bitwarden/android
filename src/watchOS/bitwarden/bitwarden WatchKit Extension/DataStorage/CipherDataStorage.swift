//
//  CipherDataStorage.swift
//  bitwarden WatchKit Extension
//
//  Created by Federico Andrés Maccaroni on 27/10/2022.
//
/*
import Foundation
import CoreData
import Combine

class CipherDataStorage: NSObject {
    var ciphers = CurrentValueSubject<[CipherEntity], Never>([])
    private var frc : NSFetchedResultsController<CipherEntity>
    
    override init() {
        frc = NSFetchedResultsController(fetchRequest: CipherEntity.fetchRequest(), managedObjectContext: CoreDataStack.shared.mainContext, sectionNameKeyPath: nil, cacheName: nil)
        super.init()
        frc.delegate = self
        do {
            try frc.performFetch()
        } catch {
            NSLog("Oops, could not fetch songs")
        }
    }
}

extension CipherDataStorage: NSFetchedResultsControllerDelegate {
    func controllerDidChangeContent(_ controller: NSFetchedResultsController<NSFetchRequestResult>) {
        guard let ciphers = controller.fetchedObjects as? [CipherEntity] else {
            return
        }
        self.ciphers.value = ciphers
    }
}
*/
