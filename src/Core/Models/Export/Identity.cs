﻿using Bit.Core.Models.View;

namespace Bit.Core.Models.Export
{
    public class Identity
    {
        public Identity()
        {
            Title = "Mr";
            FirstName = "John";
            MiddleName = "William";
            LastName = "Doe";
            Address1 = "123 Any St";
            Address2 = "Apt #123";
            Address3 = null;
            City = "New York";
            State = "NY";
            PostalCode = "10001";
            Country = "US";
            Company = "Acme Inc.";
            Email = "john@company.com";
            Phone = "5555551234";
            SSN = "000-123-4567";
            Username = "jdoe";
            PassportNumber = "US-123456789";
            LicenseNumber = "D123-12-123-12333";
        }

        public static IdentityView ToView(Identity req, IdentityView view = null)
        {
            if(view == null)
            {
                view = new IdentityView();
            }

            view.Title = req.Title;
            view.FirstName = req.FirstName;
            view.MiddleName = req.MiddleName;
            view.LastName = req.LastName;
            view.Address1 = req.Address1;
            view.Address2 = req.Address2;
            view.Address3 = req.Address3;
            view.City = req.City;
            view.State = req.State;
            view.PostalCode = req.PostalCode;
            view.Country = req.Country;
            view.Company = req.Company;
            view.Email = req.Email;
            view.Phone = req.Phone;
            view.SSN = req.SSN;
            view.Username = req.Username;
            view.PassportNumber = req.PassportNumber;
            view.LicenseNumber = req.LicenseNumber;
            return view;
        }

        public string Title { get; set; }
        public string FirstName { get; set; }
        public string MiddleName { get; set; }
        public string LastName { get; set; }
        public string Address1 { get; set; }
        public string Address2 { get; set; }
        public string Address3 { get; set; }
        public string City { get; set; }
        public string State { get; set; }
        public string PostalCode { get; set; }
        public string Country { get; set; }
        public string Company { get; set; }
        public string Email { get; set; }
        public string Phone { get; set; }
        public string SSN { get; set; }
        public string Username { get; set; }
        public string PassportNumber { get; set; }
        public string LicenseNumber { get; set; }

        public Identity(IdentityView obj)
        {
            if(obj == null)
            {
                return;
            }

            Title = obj.Title;
            FirstName = obj.FirstName;
            MiddleName = obj.MiddleName;
            LastName = obj.LastName;
            Address1 = obj.Address1;
            Address2 = obj.Address2;
            Address3 = obj.Address3;
            City = obj.City;
            State = obj.State;
            PostalCode = obj.PostalCode;
            Country = obj.Country;
            Company = obj.Company;
            Email = obj.Email;
            Phone = obj.Phone;
            SSN = obj.SSN;
            Username = obj.Username;
            PassportNumber = obj.PassportNumber;
            LicenseNumber = obj.LicenseNumber;
        }
    }
}
