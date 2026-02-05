import React, { useState } from 'react';

// Initial structure for telematics integration details
interface TelematicsIntegration {
  id?: string;
  assetUid?: string;
  name?: string;
  method?: string;
  endpoint?: string;
  queryParams?: string;
  headers?: string;
  authorizationType?: string;
  description?: string;
}

const TelematicsIntegrationMasterPage: React.FC = () => {
  const [integration, setIntegration] = useState<TelematicsIntegration>({});

  // Handler for input changes
  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setIntegration((prev) => ({ ...prev, [name]: value }));
  };

  // Placeholder for save logic
  const handleSave = () => {
    // TODO: Implement save logic (API call)
    alert('Integration details saved (mock)');
  };

  return (
    <div className="p-8 max-w-2xl mx-auto bg-gray-900 rounded-lg shadow-lg">
      <h2 className="text-2xl font-bold mb-6 text-white">Telematics Integration Details</h2>
      <form className="space-y-4">
        <input type="text" name="assetUid" value={integration.assetUid || ''} onChange={handleChange} placeholder="Asset UID" className="w-full p-2 rounded bg-gray-800 text-white" />
        <input type="text" name="name" value={integration.name || ''} onChange={handleChange} placeholder="Integration Name" className="w-full p-2 rounded bg-gray-800 text-white" />
        <select name="method" value={integration.method || ''} onChange={handleChange} className="w-full p-2 rounded bg-gray-800 text-white">
          <option value="">HTTP Method</option>
          <option value="GET">GET</option>
          <option value="POST">POST</option>
          <option value="PUT">PUT</option>
          <option value="PATCH">PATCH</option>
          <option value="DELETE">DELETE</option>
        </select>
        <input type="text" name="endpoint" value={integration.endpoint || ''} onChange={handleChange} placeholder="Endpoint URL" className="w-full p-2 rounded bg-gray-800 text-white" />
        <textarea name="queryParams" value={integration.queryParams || ''} onChange={handleChange} placeholder="Query Params (JSON or key=value)" className="w-full p-2 rounded bg-gray-800 text-white" />
        <textarea name="headers" value={integration.headers || ''} onChange={handleChange} placeholder="Headers (JSON or key=value)" className="w-full p-2 rounded bg-gray-800 text-white" />
        <select name="authorizationType" value={integration.authorizationType || ''} onChange={handleChange} className="w-full p-2 rounded bg-gray-800 text-white">
          <option value="">Authorization Type</option>
          <option value="No Auth">No Auth</option>
          <option value="Basic Auth">Basic Auth</option>
          <option value="Bearer Token">Bearer Token</option>
          <option value="JWT Bearer">JWT Bearer</option>
          <option value="Digest Auth">Digest Auth</option>
          <option value="OAuth 1.0">OAuth 1.0</option>
          <option value="OAuth 2.0">OAuth 2.0</option>
          <option value="API Key">API Key</option>
        </select>
        <textarea name="description" value={integration.description || ''} onChange={handleChange} placeholder="Description" className="w-full p-2 rounded bg-gray-800 text-white" />
        <button type="button" onClick={handleSave} className="w-full p-2 bg-blue-600 text-white rounded">Save Integration</button>
      </form>
    </div>
  );
};

export default TelematicsIntegrationMasterPage;
