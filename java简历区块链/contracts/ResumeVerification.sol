// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

contract ResumeVerification {
    enum ResumeStatus { Pending, Verified, Rejected }
    
    struct Resume {
        string name;
        string email;
        string education;
        string workExperience;
        string skills;
        string ipfsHash;
        uint256 timestamp;
        address owner;
        ResumeStatus status;
        string verificationNotes;
        uint256 lastUpdated;
    }

    mapping(string => Resume) public resumes;
    mapping(address => string[]) public userResumes;
    mapping(address => bool) public verifiers;

    event ResumeStored(string indexed resumeHash, address indexed owner, uint256 timestamp);
    event ResumeUpdated(string indexed resumeHash, address indexed owner, uint256 timestamp);
    event ResumeVerified(string indexed resumeHash, address indexed verifier, ResumeStatus status);
    event VerifierAdded(address indexed verifier);
    event VerifierRemoved(address indexed verifier);

    modifier onlyVerifier() {
        require(verifiers[msg.sender], "Not authorized as verifier");
        _;
    }

    modifier onlyOwner(string memory _resumeHash) {
        require(resumes[_resumeHash].owner == msg.sender, "Not the resume owner");
        _;
    }

    constructor() {
        verifiers[msg.sender] = true;
    }

    function addVerifier(address _verifier) external onlyVerifier {
        require(_verifier != address(0), "Invalid verifier address");
        verifiers[_verifier] = true;
        emit VerifierAdded(_verifier);
    }

    function removeVerifier(address _verifier) external onlyVerifier {
        require(_verifier != msg.sender, "Cannot remove self as verifier");
        verifiers[_verifier] = false;
        emit VerifierRemoved(_verifier);
    }

    function storeResume(
        string memory _resumeHash,
        string memory _name,
        string memory _email,
        string memory _education,
        string memory _workExperience,
        string memory _skills,
        string memory _ipfsHash
    ) external {
        require(bytes(_resumeHash).length > 0, "Resume hash cannot be empty");
        require(bytes(resumes[_resumeHash].name).length == 0, "Resume already exists");
        
        resumes[_resumeHash] = Resume({
            name: _name,
            email: _email,
            education: _education,
            workExperience: _workExperience,
            skills: _skills,
            ipfsHash: _ipfsHash,
            timestamp: block.timestamp,
            owner: msg.sender,
            status: ResumeStatus.Pending,
            verificationNotes: "",
            lastUpdated: block.timestamp
        });

        userResumes[msg.sender].push(_resumeHash);
        emit ResumeStored(_resumeHash, msg.sender, block.timestamp);
    }

    function updateResume(
        string memory _resumeHash,
        string memory _name,
        string memory _email,
        string memory _education,
        string memory _workExperience,
        string memory _skills,
        string memory _ipfsHash
    ) external onlyOwner(_resumeHash) {
        require(bytes(resumes[_resumeHash].name).length > 0, "Resume does not exist");
        
        Resume storage resume = resumes[_resumeHash];
        resume.name = _name;
        resume.email = _email;
        resume.education = _education;
        resume.workExperience = _workExperience;
        resume.skills = _skills;
        resume.ipfsHash = _ipfsHash;
        resume.lastUpdated = block.timestamp;
        resume.status = ResumeStatus.Pending;
        resume.verificationNotes = "";

        emit ResumeUpdated(_resumeHash, msg.sender, block.timestamp);
    }

    function verifyResume(
        string memory _resumeHash, 
        ResumeStatus _status,
        string memory _notes
    ) external onlyVerifier {
        require(bytes(resumes[_resumeHash].name).length > 0, "Resume does not exist");
        Resume storage resume = resumes[_resumeHash];
        resume.status = _status;
        resume.verificationNotes = _notes;
        resume.lastUpdated = block.timestamp;
        
        emit ResumeVerified(_resumeHash, msg.sender, _status);
    }

    function getResume(string memory _resumeHash) external view returns (
        string memory name,
        string memory email,
        string memory education,
        string memory workExperience,
        string memory skills,
        string memory ipfsHash,
        uint256 timestamp,
        address owner,
        ResumeStatus status,
        string memory verificationNotes,
        uint256 lastUpdated
    ) {
        Resume memory resume = resumes[_resumeHash];
        return (
            resume.name,
            resume.email,
            resume.education,
            resume.workExperience,
            resume.skills,
            resume.ipfsHash,
            resume.timestamp,
            resume.owner,
            resume.status,
            resume.verificationNotes,
            resume.lastUpdated
        );
    }

    function getUserResumes(address _user) external view returns (string[] memory) {
        return userResumes[_user];
    }
} 