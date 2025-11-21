"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import {
  FaBuilding,
  FaUsers,
  FaUserTie,
  FaPlus,
  FaEdit,
  FaTrash,
  FaCheck,
  FaTimes,
  FaCrown,
  FaUpload,
} from "react-icons/fa";
import Image from "next/image";
import { Button } from "../../../components/ui/button";
import { Input } from "../../../components/ui/input";
import SellerLayout from "../../../components/seller/SellerLayout";
import {
  becomeCompanyAdminSeller,
  getUserCompanies,
  getPendingAdminRequests,
  approveCompanyAdminByRightsId,
  denyCompanyAdminByRightsId,
  addSellerToCompany,
  removeSellerFromCompany,
  promoteSellerToAdmin,
  demoteAdminToSeller,
  getCompanyMembers,
  Company,
  AdminRightsRequest,
  CompanyAdmin,
  CompanySeller,
} from "../../api/services/sellerRole";

const companyRequestSchema = z.object({
  name: z.string().min(1, "Company name is required").max(100, "Name too long"),
  description: z.string().optional(),
  address: z.string().optional(),
  phone: z.string().optional(),
  email: z.string().email("Invalid email format").optional().or(z.literal("")),
  website: z.string().url("Invalid website URL").optional().or(z.literal("")),
});

type CompanyRequestFormValues = z.infer<typeof companyRequestSchema>;

export default function SellerRolesPage() {
  const router = useRouter();
  const [companies, setCompanies] = useState<Company[]>([]);
  const [pendingRequests, setPendingRequests] = useState<AdminRightsRequest[]>(
    []
  );
  const [selectedCompany, setSelectedCompany] = useState<Company | null>(null);
  const [companyMembers, setCompanyMembers] = useState<{
    admins: CompanyAdmin[];
    sellers: CompanySeller[];
  } | null>(null);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState<Set<number>>(new Set());
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [showMemberManagement, setShowMemberManagement] = useState(false);
  const [logoFile, setLogoFile] = useState<File | null>(null);
  const [logoPreview, setLogoPreview] = useState<string | null>(null);
  const [newMemberEmail, setNewMemberEmail] = useState("");
  const [message, setMessage] = useState<{
    type: "success" | "error" | "info";
    text: string;
  } | null>(null);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isDirty },
  } = useForm<CompanyRequestFormValues>({
    resolver: zodResolver(companyRequestSchema),
  });

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      setLoading(true);
      const [companiesData, requestsData] = await Promise.all([
        getUserCompanies(),
        getPendingAdminRequests(),
      ]);
      setCompanies(companiesData);
      setPendingRequests(requestsData);
    } catch (error) {
      console.error("Error loading role data:", error);
      setMessage({ type: "error", text: "Failed to load role data" });
    } finally {
      setLoading(false);
    }
  };

  const loadCompanyMembers = async (companyId: number) => {
    try {
      const members = await getCompanyMembers(companyId);
      setCompanyMembers(members);
    } catch (error) {
      console.error("Error loading company members:", error);
      setMessage({ type: "error", text: "Failed to load company members" });
    }
  };

  const handleLogoSelect = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (file) {
      setLogoFile(file);
      setLogoPreview(URL.createObjectURL(file));
    }
  };

  const onSubmitCompanyRequest = async (data: CompanyRequestFormValues) => {
    try {
      setActionLoading((prev) => new Set([...prev, -1]));
      await becomeCompanyAdminSeller(data, logoFile || undefined);
      setMessage({
        type: "success",
        text: "Company admin request submitted successfully!",
      });
      setShowCreateForm(false);
      reset();
      setLogoFile(null);
      setLogoPreview(null);
      await loadData();
    } catch (error: unknown) {
      console.error("Error submitting company request:", error);
      const errorMessage =
        (error as { response?: { data?: { message?: string } } })?.response
          ?.data?.message || "Failed to submit company request";
      setMessage({ type: "error", text: errorMessage });
    } finally {
      setActionLoading((prev) => {
        const newSet = new Set(prev);
        newSet.delete(-1);
        return newSet;
      });
    }
  };

  const handleApproveRequest = async (requestId: number) => {
    try {
      setActionLoading((prev) => new Set([...prev, requestId]));
      await approveCompanyAdminByRightsId(requestId);
      setMessage({ type: "success", text: "Request approved successfully" });
      await loadData();
    } catch (error) {
      console.error("Error approving request:", error);
      setMessage({ type: "error", text: "Failed to approve request" });
    } finally {
      setActionLoading((prev) => {
        const newSet = new Set(prev);
        newSet.delete(requestId);
        return newSet;
      });
    }
  };

  const handleDenyRequest = async (requestId: number) => {
    try {
      setActionLoading((prev) => new Set([...prev, requestId]));
      await denyCompanyAdminByRightsId(requestId);
      setMessage({ type: "success", text: "Request denied successfully" });
      await loadData();
    } catch (error) {
      console.error("Error denying request:", error);
      setMessage({ type: "error", text: "Failed to deny request" });
    } finally {
      setActionLoading((prev) => {
        const newSet = new Set(prev);
        newSet.delete(requestId);
        return newSet;
      });
    }
  };

  const handleAddMember = async () => {
    if (!selectedCompany || !newMemberEmail.trim()) return;

    try {
      setActionLoading((prev) => new Set([...prev, -2]));
      await addSellerToCompany(selectedCompany.id, newMemberEmail.trim());
      setMessage({ type: "success", text: "Member added successfully" });
      setNewMemberEmail("");
      await loadCompanyMembers(selectedCompany.id);
    } catch (error) {
      console.error("Error adding member:", error);
      setMessage({ type: "error", text: "Failed to add member" });
    } finally {
      setActionLoading((prev) => {
        const newSet = new Set(prev);
        newSet.delete(-2);
        return newSet;
      });
    }
  };

  const handlePromoteToAdmin = async (email: string) => {
    if (!selectedCompany) return;

    try {
      setActionLoading((prev) => new Set([...prev, email.length]));
      await promoteSellerToAdmin(selectedCompany.id, email);
      setMessage({
        type: "success",
        text: "Member promoted to admin successfully",
      });
      await loadCompanyMembers(selectedCompany.id);
    } catch (error) {
      console.error("Error promoting member:", error);
      setMessage({ type: "error", text: "Failed to promote member" });
    } finally {
      setActionLoading((prev) => {
        const newSet = new Set(prev);
        newSet.delete(email.length);
        return newSet;
      });
    }
  };

  const handleDemoteToSeller = async (email: string) => {
    if (!selectedCompany) return;

    try {
      setActionLoading((prev) => new Set([...prev, email.length]));
      await demoteAdminToSeller(selectedCompany.id, email);
      setMessage({
        type: "success",
        text: "Admin demoted to seller successfully",
      });
      await loadCompanyMembers(selectedCompany.id);
    } catch (error) {
      console.error("Error demoting admin:", error);
      setMessage({ type: "error", text: "Failed to demote admin" });
    } finally {
      setActionLoading((prev) => {
        const newSet = new Set(prev);
        newSet.delete(email.length);
        return newSet;
      });
    }
  };

  const handleRemoveMember = async (email: string) => {
    if (
      !selectedCompany ||
      !confirm("Are you sure you want to remove this member?")
    )
      return;

    try {
      setActionLoading((prev) => new Set([...prev, email.length + 1000]));
      await removeSellerFromCompany(selectedCompany.id, email);
      setMessage({ type: "success", text: "Member removed successfully" });
      await loadCompanyMembers(selectedCompany.id);
    } catch (error) {
      console.error("Error removing member:", error);
      setMessage({ type: "error", text: "Failed to remove member" });
    } finally {
      setActionLoading((prev) => {
        const newSet = new Set(prev);
        newSet.delete(email.length + 1000);
        return newSet;
      });
    }
  };

  return (
    <SellerLayout
      activeNav="roles"
      title="Role Management"
      description="Manage your company and seller roles"
    >
      {/* Message Display */}
      {message && (
        <div
          className={`mb-6 p-4 rounded-md ${
            message.type === "success"
              ? "bg-green-50 text-green-700 border border-green-200"
              : message.type === "info"
              ? "bg-blue-50 text-blue-700 border border-blue-200"
              : "bg-red-50 text-red-700 border border-red-200"
          }`}
        >
          {message.text}
        </div>
      )}

      <div className="space-y-6">
        {/* Pending Requests */}
        {pendingRequests.length > 0 && (
          <div className="bg-white rounded-lg shadow-sm border p-6">
            <h3 className="text-lg font-medium text-gray-900 mb-4">
              Pending Admin Requests
            </h3>
            <div className="space-y-4">
              {pendingRequests.map((request) => (
                <div
                  key={request.id}
                  className="flex items-center justify-between p-4 bg-yellow-50 border border-yellow-200 rounded-lg"
                >
                  <div>
                    <h4 className="font-medium text-gray-900">
                      Company Admin Request
                      {request.companyName && ` - ${request.companyName}`}
                    </h4>
                    <p className="text-sm text-gray-600">
                      Requested:{" "}
                      {new Date(request.requestedAt).toLocaleDateString()}
                    </p>
                  </div>
                  <div className="flex gap-2">
                    <Button
                      onClick={() => handleApproveRequest(request.id)}
                      loading={actionLoading.has(request.id)}
                      size="sm"
                      className="bg-green-600 hover:bg-green-700"
                    >
                      <FaCheck className="w-4 h-4 mr-2" />
                      Approve
                    </Button>
                    <Button
                      onClick={() => handleDenyRequest(request.id)}
                      loading={actionLoading.has(request.id)}
                      size="sm"
                      variant="outline"
                      className="border-red-300 text-red-600 hover:bg-red-50"
                    >
                      <FaTimes className="w-4 h-4 mr-2" />
                      Deny
                    </Button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Companies */}
        <div className="bg-white rounded-lg shadow-sm border p-6">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-lg font-medium text-gray-900">
              Your Companies
            </h3>
            <Button
              onClick={() => setShowCreateForm(!showCreateForm)}
              className="bg-blue-600 hover:bg-blue-700"
            >
              <FaPlus className="w-4 h-4 mr-2" />
              Request Company Admin
            </Button>
          </div>

          {/* Create Company Form */}
          {showCreateForm && (
            <div className="mb-6 p-4 bg-gray-50 rounded-lg border">
              <h4 className="font-medium text-gray-900 mb-4">
                Request Company Admin Role
              </h4>
              <form
                onSubmit={handleSubmit(onSubmitCompanyRequest)}
                className="space-y-4"
              >
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      Company Name *
                    </label>
                    <Input
                      {...register("name")}
                      placeholder="Enter company name"
                      error={errors.name?.message}
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      Email
                    </label>
                    <Input
                      {...register("email")}
                      type="email"
                      placeholder="company@example.com"
                      error={errors.email?.message}
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      Phone
                    </label>
                    <Input
                      {...register("phone")}
                      placeholder="+1 (555) 123-4567"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      Website
                    </label>
                    <Input
                      {...register("website")}
                      placeholder="https://company.com"
                      error={errors.website?.message}
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Description
                  </label>
                  <textarea
                    {...register("description")}
                    rows={3}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                    placeholder="Describe your company..."
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Address
                  </label>
                  <Input
                    {...register("address")}
                    placeholder="Company address"
                  />
                </div>

                {/* Logo Upload */}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Company Logo
                  </label>
                  <div className="flex items-center gap-4">
                    {logoPreview && (
                      <div className="w-16 h-16 relative rounded-lg overflow-hidden border">
                        <Image
                          src={logoPreview}
                          alt="Logo preview"
                          fill
                          className="object-cover"
                        />
                      </div>
                    )}
                    <div>
                      <input
                        type="file"
                        accept="image/*"
                        onChange={handleLogoSelect}
                        className="hidden"
                        id="logo-upload"
                      />
                      <label
                        htmlFor="logo-upload"
                        className="cursor-pointer inline-flex items-center px-4 py-2 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50"
                      >
                        <FaUpload className="w-4 h-4 mr-2" />
                        Upload Logo
                      </label>
                    </div>
                  </div>
                </div>

                <div className="flex justify-end gap-3">
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() => {
                      setShowCreateForm(false);
                      reset();
                      setLogoFile(null);
                      setLogoPreview(null);
                    }}
                  >
                    Cancel
                  </Button>
                  <Button
                    type="submit"
                    loading={actionLoading.has(-1)}
                    disabled={!isDirty || actionLoading.has(-1)}
                    className="bg-blue-600 hover:bg-blue-700"
                  >
                    Submit Request
                  </Button>
                </div>
              </form>
            </div>
          )}

          {/* Companies List */}
          {loading ? (
            <div className="space-y-4">
              {[1, 2].map((i) => (
                <div key={i} className="animate-pulse">
                  <div className="h-20 bg-gray-200 rounded-lg"></div>
                </div>
              ))}
            </div>
          ) : companies.length > 0 ? (
            <div className="space-y-4">
              {companies.map((company) => (
                <div
                  key={company.id}
                  className="border border-gray-200 rounded-lg p-4 hover:border-gray-300 transition-colors"
                >
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-4">
                      <div className="w-12 h-12 bg-gray-100 rounded-lg flex items-center justify-center">
                        {company.logoUrl ? (
                          <Image
                            src={company.logoUrl}
                            alt={company.name}
                            width={48}
                            height={48}
                            className="rounded-lg object-cover"
                          />
                        ) : (
                          <FaBuilding className="w-6 h-6 text-gray-400" />
                        )}
                      </div>
                      <div>
                        <h4 className="font-medium text-gray-900">
                          {company.name}
                        </h4>
                        <div className="flex items-center gap-4 text-sm text-gray-600">
                          <span>
                            <FaUserTie className="inline w-3 h-3 mr-1" />
                            {company.admins.length} Admin
                            {company.admins.length !== 1 ? "s" : ""}
                          </span>
                          <span>
                            <FaUsers className="inline w-3 h-3 mr-1" />
                            {company.sellers.length} Seller
                            {company.sellers.length !== 1 ? "s" : ""}
                          </span>
                          <span
                            className={`px-2 py-1 text-xs rounded-full ${
                              company.isActive
                                ? "bg-green-100 text-green-800"
                                : "bg-gray-100 text-gray-600"
                            }`}
                          >
                            {company.isActive ? "Active" : "Inactive"}
                          </span>
                        </div>
                      </div>
                    </div>
                    <div className="flex gap-2">
                      <Button
                        onClick={() => {
                          setSelectedCompany(company);
                          setShowMemberManagement(true);
                          loadCompanyMembers(company.id);
                        }}
                        variant="outline"
                        size="sm"
                      >
                        <FaUsers className="w-4 h-4 mr-2" />
                        Manage Members
                      </Button>
                      <Button
                        onClick={() =>
                          router.push(`/seller/company/${company.id}`)
                        }
                        variant="outline"
                        size="sm"
                      >
                        <FaEdit className="w-4 h-4 mr-2" />
                        View Details
                      </Button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="text-center py-8">
              <FaBuilding className="w-12 h-12 text-gray-300 mx-auto mb-4" />
              <h4 className="text-lg font-medium text-gray-900 mb-2">
                No companies yet
              </h4>
              <p className="text-gray-600 mb-4">
                Request company admin role to start managing a company
              </p>
            </div>
          )}
        </div>
      </div>

      {/* Member Management Modal */}
      {showMemberManagement && selectedCompany && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-lg shadow-xl max-w-4xl w-full max-h-[80vh] overflow-y-auto">
            <div className="p-6 border-b border-gray-200">
              <div className="flex items-center justify-between">
                <h3 className="text-lg font-medium text-gray-900">
                  Manage Members - {selectedCompany.name}
                </h3>
                <button
                  onClick={() => {
                    setShowMemberManagement(false);
                    setSelectedCompany(null);
                    setCompanyMembers(null);
                  }}
                  className="text-gray-400 hover:text-gray-600"
                >
                  <FaTimes className="w-5 h-5" />
                </button>
              </div>
            </div>

            <div className="p-6">
              {/* Add Member */}
              <div className="mb-6">
                <h4 className="font-medium text-gray-900 mb-3">
                  Add New Member
                </h4>
                <div className="flex gap-3">
                  <Input
                    value={newMemberEmail}
                    onChange={(e) => setNewMemberEmail(e.target.value)}
                    placeholder="Enter member email"
                    className="flex-1"
                  />
                  <Button
                    onClick={handleAddMember}
                    loading={actionLoading.has(-2)}
                    disabled={!newMemberEmail.trim()}
                  >
                    <FaPlus className="w-4 h-4 mr-2" />
                    Add Member
                  </Button>
                </div>
              </div>

              {/* Members List */}
              {companyMembers && (
                <div className="space-y-6">
                  {/* Admins */}
                  <div>
                    <h4 className="font-medium text-gray-900 mb-3 flex items-center">
                      <FaCrown className="w-4 h-4 mr-2 text-yellow-500" />
                      Company Admins ({companyMembers.admins.length})
                    </h4>
                    <div className="space-y-2">
                      {companyMembers.admins.map((admin) => (
                        <div
                          key={admin.id}
                          className="flex items-center justify-between p-3 bg-yellow-50 border border-yellow-200 rounded-lg"
                        >
                          <div>
                            <p className="font-medium text-gray-900">
                              {admin.firstName} {admin.lastName}
                            </p>
                            <p className="text-sm text-gray-600">
                              {admin.email}
                            </p>
                          </div>
                          <div className="flex gap-2">
                            <Button
                              onClick={() => handleDemoteToSeller(admin.email)}
                              loading={actionLoading.has(admin.email.length)}
                              size="sm"
                              variant="outline"
                              className="text-orange-600 border-orange-300 hover:bg-orange-50"
                            >
                              Demote to Seller
                            </Button>
                            <Button
                              onClick={() => handleRemoveMember(admin.email)}
                              loading={actionLoading.has(
                                admin.email.length + 1000
                              )}
                              size="sm"
                              variant="outline"
                              className="text-red-600 border-red-300 hover:bg-red-50"
                            >
                              <FaTrash className="w-4 h-4" />
                            </Button>
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>

                  {/* Sellers */}
                  <div>
                    <h4 className="font-medium text-gray-900 mb-3 flex items-center">
                      <FaUsers className="w-4 h-4 mr-2 text-blue-500" />
                      Sellers ({companyMembers.sellers.length})
                    </h4>
                    <div className="space-y-2">
                      {companyMembers.sellers.map((seller) => (
                        <div
                          key={seller.id}
                          className="flex items-center justify-between p-3 bg-blue-50 border border-blue-200 rounded-lg"
                        >
                          <div>
                            <p className="font-medium text-gray-900">
                              {seller.firstName} {seller.lastName}
                            </p>
                            <p className="text-sm text-gray-600">
                              {seller.email}
                            </p>
                          </div>
                          <div className="flex gap-2">
                            <Button
                              onClick={() => handlePromoteToAdmin(seller.email)}
                              loading={actionLoading.has(seller.email.length)}
                              size="sm"
                              variant="outline"
                              className="text-green-600 border-green-300 hover:bg-green-50"
                            >
                              <FaCrown className="w-4 h-4 mr-2" />
                              Promote to Admin
                            </Button>
                            <Button
                              onClick={() => handleRemoveMember(seller.email)}
                              loading={actionLoading.has(
                                seller.email.length + 1000
                              )}
                              size="sm"
                              variant="outline"
                              className="text-red-600 border-red-300 hover:bg-red-50"
                            >
                              <FaTrash className="w-4 h-4" />
                            </Button>
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </SellerLayout>
  );
}
